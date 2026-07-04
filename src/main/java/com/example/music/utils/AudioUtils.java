package com.example.music.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import org.apache.tika.io.TikaInputStream;

import java.io.File;

/**
 * 音频工具类
 * <p>
 * 提供从音频文件字节中提取时长（秒）的工具方法。
 * 支持 MP3（通过 Xing/Info 头或帧采样率估算）、AAC（MP4/M4A 容器）、
 * FLAC、WAV、OGG 等格式（通过 Apache Tika 兜底解析）。
 * 轻量解析器处理标准 MP3/AAC 文件，Tika 作为后备处理其他格式。
 */
@Slf4j
public class AudioUtils {

    /** 最多读取前 1MB 用于解析 MP3 头部（足够跳过 ID3v2 标签并找到 Xing 头） */
    public static final int MAX_HEADER_BYTES = 1024 * 1024;

    /**
     * 从音频文件的字节数据中提取时长（秒）—— 完整文件字节版本
     * <p>
     * 自动检测格式：
     * <ul>
     *   <li>MP3 — 跳过 ID3v2 标签头，查找第一个 MPEG 帧的 Xing/Info 扩展头获取帧数</li>
     *   <li>AAC/MP4/M4A — 解析 moov box 中的 mvhd atom 获取时长和时基</li>
     * </ul>
     *
     * @param audioBytes 音频文件的完整字节数据
     * @return 时长（秒），解析失败时返回 0（不影响上传主流程）
     */
    public static int extractDuration(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length < 10) {
            return 0;
        }
        return extractDuration(audioBytes, audioBytes.length);
    }

    /**
     * 从音频文件提取时长（秒）—— 高效版本，只需读头部分 + 文件总大小
     * <p>
     * 对于 MP3，只需要文件头部（跳过 ID3v2 + 第一个 MPEG 帧）即可解析时长。
     * 文件总大小用于 CBR 比特率估算回退。避免将整个大文件读入内存。
     *
     * @param headerBytes 音频文件的头部字节（至少前 1MB，或整个文件如果更小）
     * @param fileSize    文件总字节数（用于 CBR 比特率回退估算）
     * @return 时长（秒），解析失败时返回 0
     */
    public static int extractDuration(byte[] headerBytes, long fileSize) {
        if (headerBytes == null || headerBytes.length < 10) {
            return 0;
        }

        try {
            // 仅用头部解析器处理标准 MP3（Xing 头）和 MP4（mvhd box）
            // 对于需要完整文件才能准确计算的格式（无 Xing 头的 VBR MP3、FLAC、WAV 等），
            // 请使用 extractDurationFromFile(String) 方法
            if (isMp4Container(headerBytes)) {
                return parseMp4Duration(headerBytes);
            }
            return parseMp3Duration(headerBytes, fileSize);
        } catch (Exception e) {
            log.warn("头部解析时长失败: {}", e.getMessage());
            return 0;
        }
    }

    // ==================== 格式检测 ====================

    /**
     * 检测是否为 MP4/M4A 容器（通过 ftyp box 魔数）
     */
    private static boolean isMp4Container(byte[] bytes) {
        // MP4 文件的第 5-8 字节为 "ftyp"（box 结构: 4字节size + 4字节type）
        return bytes.length >= 8
                && bytes[4] == 0x66   // 'f'
                && bytes[5] == 0x74   // 't'
                && bytes[6] == 0x79   // 'y'
                && bytes[7] == 0x70;  // 'p'
    }

    // ==================== MP4/AAC 时长解析 ====================

    /**
     * 解析 MP4/M4A 容器中的时长
     * <p>
     * MP4 文件由一系列 box（atom）组成，每 4 字节为 size（大端），后 4 字节为 type。
     * 主要遍历 moov → mvhd 子 box，读取 timescale 和 duration。
     */
    private static int parseMp4Duration(byte[] bytes) {
        int pos = 0;
        while (pos + 8 <= bytes.length) {
            // 读取 box size（大端 4 字节）
            int boxSize = readInt32BigEndian(bytes, pos);
            if (boxSize < 8) {
                break; // 无效 box
            }

            // 读取 box type
            String boxType = new String(bytes, pos + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);

            // 找到 moov box
            if ("moov".equals(boxType)) {
                // 在 moov 内部递归查找 mvhd
                return parseMvhdInMoov(bytes, pos + 8, pos + boxSize);
            }

            // 不相关的 box，跳过
            if (boxSize == 0) {
                break; // boxSize=0 表示最后一个 box
            }
            pos += boxSize;
        }
        return 0;
    }

    /**
     * 在 moov box 内部查找 mvhd atom
     */
    private static int parseMvhdInMoov(byte[] bytes, int start, int end) {
        int pos = start;
        while (pos + 8 <= end) {
            int boxSize = readInt32BigEndian(bytes, pos);
            if (boxSize < 8) {
                break;
            }

            String boxType = new String(bytes, pos + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);

            if ("mvhd".equals(boxType)) {
                return readMvhdDuration(bytes, pos, boxSize);
            }

            if (boxSize == 0) {
                break;
            }
            pos += boxSize;
        }
        return 0;
    }

    /**
     * 从 mvhd atom 读取时长
     * <p>
     * mvhd 结构（v0 32 位，v1 64 位）：
     * - version(1) + flags(3)
     * - created_time(4/8) + modified_time(4/8)
     * - timescale(4)
     * - duration(4/8)
     * v0: 偏移 12 为 timescale，16 为 duration（32 位）
     * v1: 偏移 20 为 timescale，24 为 duration（64 位）
     */
    private static int readMvhdDuration(byte[] bytes, int boxStart, int boxSize) {
        if (boxSize < 32) {
            return 0;
        }

        int version = bytes[boxStart + 8] & 0xFF;
        long timescale;
        long duration;

        if (version == 0) {
            // v0: 32 位
            timescale = readInt32BigEndian(bytes, boxStart + 12);
            duration = readInt32BigEndian(bytes, boxStart + 16);
        } else if (version == 1) {
            // v1: 64 位
            timescale = readInt32BigEndian(bytes, boxStart + 20);
            duration = readInt64BigEndian(bytes, boxStart + 24);
        } else {
            return 0;
        }

        if (timescale == 0) {
            return 0;
        }

        long seconds = duration / timescale;
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }

    // ==================== MP3 时长解析 ====================

    /**
     * MPEG 版本与采样率映射表
     * 索引: (versionBits << 2) | sampleRateBits
     */
    private static final int[][] MPEG_SAMPLE_RATES = {
            // versionBits: 3=MPEG1, 2=MPEG2, 0=MPEG2.5
            {11025, 12000, 8000, 0},   // MPEG2.5
            {0, 0, 0, 0},              // reserved
            {22050, 24000, 16000, 0},  // MPEG2
            {44100, 48000, 32000, 0}   // MPEG1
    };

    /**
     * 每帧采样数
     */
    private static final int[] SAMPLES_PER_FRAME = {
            0,    // reserved
            576,  // MPEG2/2.5 Layer3
            1152, // MPEG1 Layer3
            0     // reserved
    };

    /**
     * MPEG Layer III side info 大小（取决于版本和声道模式）
     * 索引: [versionBits][channelMode == 3 ? 1 : 0]
     * channelMode: 0/1/2=stereo/dual, 3=mono
     */
    private static final int[][] SIDE_INFO_SIZE = {
            {17, 9},   // MPEG2.5  stereo/mono
            {0, 0},    // reserved
            {17, 9},   // MPEG2     stereo/mono
            {32, 17}   // MPEG1     stereo/mono
    };

    /**
     * 解析 MP3 时长（使用完整的文件字节）
     */
    private static int parseMp3Duration(byte[] bytes) {
        return parseMp3Duration(bytes, bytes.length);
    }

    /**
     * 解析 MP3 时长
     * <p>
     * 通过以下步骤：
     * 1. 跳过可选的 ID3v2 标签头
     * 2. 定位第一个 MPEG 帧同步字（0xFFF）
     * 3. 解析帧头获取版本/采样率
     * 4. 在第 1 帧中查找 Xing/Info 头以获得精确帧数
     * 5. 若无 Xing 头，通过文件大小估算
     *
     * @param bytes        文件头部字节（需至少覆盖到第一个 MPEG 帧的 Xing 头区域）
     * @param totalFileSize 文件总字节数（用于 CBR 比特率回退估算）
     */
    private static int parseMp3Duration(byte[] bytes, long totalFileSize) {
        int offset = 0;
        int length = bytes.length;

        // 1. 跳过 ID3v2 标签头
        if (length >= 10 && bytes[0] == 0x49 && bytes[1] == 0x44 && bytes[2] == 0x33) {
            int id3Size = ((bytes[6] & 0x7F) << 21) |
                          ((bytes[7] & 0x7F) << 14) |
                          ((bytes[8] & 0x7F) << 7) |
                          (bytes[9] & 0x7F);
            offset = 10 + id3Size;
        }

        // 2. 定位第一个 MPEG 帧同步字
        int frameStart = findMpegSync(bytes, offset);
        if (frameStart < 0) {
            return 0;
        }

        // 3. 解析帧头
        int header = readInt32BigEndian(bytes, frameStart);
        int versionBits = (header >> 19) & 0x03;
        int layerBits = (header >> 17) & 0x03;
        int sampleRateBits = (header >> 10) & 0x03;

        // 验证有效帧
        if (versionBits == 1 || layerBits != 1) {
            // versionBits=1 为保留，layerBits=1 为 Layer III
            return 0;
        }

        int sampleRate = MPEG_SAMPLE_RATES[versionBits][sampleRateBits];
        int samplesPerFrame = SAMPLES_PER_FRAME[versionBits];

        if (sampleRate == 0 || samplesPerFrame == 0) {
            return 0;
        }

        // 4. 查找 Xing/Info 头（在第一个 MPEG 帧的 side info 后面）
        //    先计算正确的偏移：帧头(4) + CRC(0或2) + side info(根据版本和声道)
        int crcSize = ((header >> 16) & 0x01) == 0 ? 2 : 0; // protection=0 则有 2 字节 CRC
        int channelMode = (header >> 6) & 0x03;             // 3=mono, 0/1/2=stereo
        int sideInfoSize = SIDE_INFO_SIZE[versionBits][channelMode == 3 ? 1 : 0];
        int xingOffset = frameStart + 4 + crcSize + sideInfoSize;

        // 尝试从计算出的位置读取 Xing/Info 头
        if (xingOffset + 12 <= length) {
            String tag = new String(bytes, xingOffset, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if ("Xing".equals(tag) || "Info".equals(tag)) {
                int flags = readInt32BigEndian(bytes, xingOffset + 4);
                if ((flags & 0x01) != 0) {
                    int frames = readInt32BigEndian(bytes, xingOffset + 8);
                    if (frames > 0) {
                        long durationMs = (long) frames * samplesPerFrame * 1000L / sampleRate;
                        return (int) (durationMs / 1000L);
                    }
                }
            }
        }

        // 如果计算位置没找到，再尝试几种常见偏移（兼容编码器差异）
        // 常见偏移: MPEG1=36(无CRC)/38(有CRC) 或 21/23; MPEG2=21(无CRC)/23(有CRC) 或 13/15
        int[] fallbackOffsets = {36, 38, 21, 23, 13, 15};
        for (int offs : fallbackOffsets) {
            int tryOffset = frameStart + offs;
            if (tryOffset + 12 <= length && tryOffset != xingOffset) {
                String tag = new String(bytes, tryOffset, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if ("Xing".equals(tag) || "Info".equals(tag)) {
                    int flags = readInt32BigEndian(bytes, tryOffset + 4);
                    if ((flags & 0x01) != 0) {
                        int frames = readInt32BigEndian(bytes, tryOffset + 8);
                        if (frames > 0) {
                            long durationMs = (long) frames * samplesPerFrame * 1000L / sampleRate;
                            return (int) (durationMs / 1000L);
                        }
                    }
                }
            }
        }

        // 5. 没有 Xing 头，用文件大小估算（已知第一个帧的比特率）
        //    使用 totalFileSize 而非 bytes.length，避免头部截断导致估算偏小
        int bitrateIndex = (header >> 12) & 0x0F;
        int bitrate = getBitrate(versionBits, bitrateIndex);
        if (bitrate > 0) {
            long audioDataSize = totalFileSize > frameStart ? totalFileSize - frameStart : totalFileSize;
            // duration = totalBytes * 8 / (bitrate * 1000)
            long durationSec = (audioDataSize * 8L) / (bitrate * 1000L);
            return Math.max(1, (int) durationSec);
        }

        return 0;
    }

    /**
     * 查找 MPEG 帧同步字（0xFFF）
     */
    private static int findMpegSync(byte[] bytes, int start) {
        for (int i = start; i < bytes.length - 1; i++) {
            if (bytes[i] == (byte) 0xFF && (bytes[i + 1] & 0xF0) == 0xF0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据 MPEG 版本和比特率索引获取比特率（kbps）
     */
    private static int getBitrate(int versionBits, int index) {
        // MPEG1 Layer3 比特率表（索引 1-14）
        if (versionBits == 3) {
            return switch (index) {
                case 1 -> 32;  case 2 -> 40;  case 3 -> 48;
                case 4 -> 56;  case 5 -> 64;  case 6 -> 80;
                case 7 -> 96;  case 8 -> 112; case 9 -> 128;
                case 10 -> 160; case 11 -> 192; case 12 -> 224;
                case 13 -> 256; case 14 -> 320;
                default -> 0;
            };
        }
        // MPEG2/2.5 Layer3 比特率表
        return switch (index) {
            case 1 -> 8;   case 2 -> 16;  case 3 -> 24;
            case 4 -> 32;  case 5 -> 40;  case 6 -> 48;
            case 7 -> 56;  case 8 -> 64;  case 9 -> 80;
            case 10 -> 96; case 11 -> 112; case 12 -> 128;
            case 13 -> 144; case 14 -> 160;
            default -> 0;
        };
    }

    // ==================== Tika 完整文件解析 ====================

    /**
     * 使用 Apache Tika 从完整音频文件中提取时长（秒）
     * <p>
     * 通过 TikaInputStream 读取完整文件，支持所有 Tika 能识别的音频格式：
     * MP3（含无 Xing 头的 VBR）、AAC/MP4、FLAC、WAV、OGG、WMA 等。
     * 相比 {@link #extractDuration(byte[], long)} 只读 1MB 头部，
     * 此方法提供 100% 准确的时长数据。
     *
     * @param filePath 音频文件的绝对路径
     * @return 时长（秒），解析失败返回 0
     */
    public static int extractDurationFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            log.warn("音频文件不存在: {}", filePath);
            return 0;
        }

        try (TikaInputStream tis = TikaInputStream.get(file)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_LENGTH, String.valueOf(file.length()));
            ParseContext context = new ParseContext();

            parser.parse(tis, handler, metadata, context);

            // Tika 在不同格式下将时长写入不同元数据字段
            String[] durationKeys = {"xmpDM:duration", "duration", "dc:duration"};
            for (String key : durationKeys) {
                String val = metadata.get(key);
                if (val != null) {
                    try {
                        double duration = Double.parseDouble(val);
                        if (duration > 0) {
                            log.debug("Tika 完整文件解析成功: {} -> {} 秒", filePath, (int) Math.round(duration));
                            return (int) Math.round(duration);
                        }
                    } catch (NumberFormatException ignored) {
                        // 继续尝试下一个 key
                    }
                }
            }
            log.warn("Tika 完整文件解析未找到时长元数据: {}", filePath);
        } catch (Exception e) {
            log.warn("Tika 完整文件解析异常: {}, error={}", filePath, e.getMessage());
        }
        return 0;
    }

    // ==================== 字节工具 ====================

    /**
     * 以大端序读取 4 字节整数
     */
    private static int readInt32BigEndian(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8) |
               (bytes[offset + 3] & 0xFF);
    }

    /**
     * 以大端序读取 8 字节整数
     */
    private static long readInt64BigEndian(byte[] bytes, int offset) {
        return ((long) (bytes[offset] & 0xFF) << 56) |
               ((long) (bytes[offset + 1] & 0xFF) << 48) |
               ((long) (bytes[offset + 2] & 0xFF) << 40) |
               ((long) (bytes[offset + 3] & 0xFF) << 32) |
               ((long) (bytes[offset + 4] & 0xFF) << 24) |
               ((long) (bytes[offset + 5] & 0xFF) << 16) |
               ((long) (bytes[offset + 6] & 0xFF) << 8) |
               ((long) (bytes[offset + 7] & 0xFF));
    }
}