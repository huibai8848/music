package com.example.music.mapper;

import com.example.music.entity.RechargeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 充值记录 Mapper
 */
@Mapper
public interface RechargeRecordMapper {
    int insert(RechargeRecord record);
    List<RechargeRecord> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    int countByUserId(@Param("userId") Long userId);
}