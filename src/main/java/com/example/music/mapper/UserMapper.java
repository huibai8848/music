package com.example.music.mapper;

import com.example.music.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {

    /** 根据 ID 查询 */
    User selectById(@Param("id") Long id);

    /** 根据邮箱查询 */
    User selectByEmail(@Param("email") String email);

    /** 新增用户 */
    int insert(User user);

    /** 更新用户信息 */
    int update(User user);

    /** 更新密码 */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 更新会员信息 */
    int updateVip(@Param("id") Long id, @Param("role") String role, @Param("vipExpireTime") java.time.LocalDateTime vipExpireTime);

    /** 更新状态（封禁/解封） */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 统计总用户数 */
    long countTotal();

    /** 统计今日新增 */
    long countToday();

    /** 统计指定日期的注册用户数 */
    long countByDate(@Param("date") java.time.LocalDate date);

    /** 统计会员总数 */
    long countVip();

    /** 分页查询所有用户（管理端，支持按昵称/邮箱搜索） */
    List<User> selectAll(@Param("keyword") String keyword,
                         @Param("offset") int offset,
                         @Param("limit") int limit);

    /** 统计所有用户数（管理端，支持按昵称/邮箱搜索） */
    long countAll(@Param("keyword") String keyword);

    /** 按角色查询用户列表 */
    List<User> selectByRole(@Param("role") String role);

    /** 按多个角色查询用户列表（用于群发通知等场景） */
    List<User> selectByRoles(@Param("roles") List<String> roles);
}