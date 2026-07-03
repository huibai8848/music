package com.example.music.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * RSA 加密工具类
 * <p>
 * 用于登录密码的加密传输：前端使用公钥加密密码，后端使用私钥解密。
 * 系统启动时自动生成 2048 位 RSA 密钥对。
 */
@Slf4j
@Component
public class RsaUtil {

    /** RSA 密钥对 */
    private PrivateKey privateKey;
    private PublicKey publicKey;

    /** 公钥的 Base64 编码字符串（供前端使用） */
    private String publicKeyBase64;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, new SecureRandom());
            KeyPair pair = generator.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            log.info("RSA 密钥对初始化完成，公钥长度: {} 字节", publicKeyBase64.length());
        } catch (NoSuchAlgorithmException e) {
            log.error("RSA 密钥对生成失败", e);
            throw new RuntimeException("RSA 密钥对生成失败", e);
        }
    }

    /**
     * 获取公钥（Base64 编码）
     */
    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    /**
     * 解密 RSA 加密的密文
     *
     * @param encryptedBase64 Base64 编码的 RSA 密文
     * @return 解密后的明文字符串
     */
    public String decrypt(String encryptedBase64) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA 解密失败", e);
            throw new RuntimeException("密码解密失败", e);
        }
    }
}