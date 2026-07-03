package com.example.music.controller;

import com.example.music.dto.PasswordDTO;
import com.example.music.entity.User;
import com.example.music.service.UserService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import com.example.music.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * <p>
 * 处理用户个人信息、密码修改、会员充值、公开主页等操作。
 *
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public R<UserVO> getCurrentUser() {
        Long userId = RequestContext.getUserId();
        UserVO userVO = userService.getCurrentUser(userId);
        return R.ok(userVO);
    }

    /**
     * 修改个人信息
     */
    @PutMapping("/me")
    public R<UserVO> updateProfile(@RequestBody User user) {
        Long userId = RequestContext.getUserId();
        UserVO userVO = userService.updateProfile(userId, user);
        return R.ok("修改成功", userVO);
    }

    /**
     * 修改密码
     */
    @PutMapping("/me/password")
    public R<Object> changePassword(@Valid @RequestBody PasswordDTO dto) {
        Long userId = RequestContext.getUserId();
        userService.changePassword(userId, dto);
        return R.ok("密码修改成功");
    }

    /**
     * 充值会员
     *
     * @param plan      套餐类型：MONTHLY / QUARTERLY / YEARLY
     * @param paymentId 支付凭证 ID（生产环境必填，开发环境可空）
     */
    @PostMapping("/me/membership")
    public R<UserVO> rechargeMembership(@RequestParam String plan,
                                        @RequestParam(required = false) String paymentId) {
        Long userId = RequestContext.getUserId();
        UserVO userVO = userService.rechargeMembership(userId, plan, paymentId);
        return R.ok("充值成功", userVO);
    }

    /**
     * 获取用户公开主页
     */
    @GetMapping("/{id}")
    public R<UserVO> getPublicProfile(@PathVariable Long id) {
        UserVO userVO = userService.getPublicProfile(id);
        return R.ok(userVO);
    }
}