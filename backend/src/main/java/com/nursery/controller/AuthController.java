package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.config.BizException;
import com.nursery.config.JwtUtil;
import com.nursery.entity.User;
import com.nursery.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public record LoginReq(@NotBlank(message = "请输入用户名") String username,
                           @NotBlank(message = "请输入密码") String password) {}

    public record RegisterReq(@NotBlank(message = "请输入用户名") String username,
                              @NotBlank(message = "请输入密码") String password,
                              @NotBlank(message = "请输入姓名") String name,
                              String phone) {}

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq req) {
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> BizException.unauthorized("用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        return tokenPayload(user);
    }

    /** 家长自助注册（园内角色由园方预置） */
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterReq req) {
        if (userRepo.existsByUsername(req.username())) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setRole(User.Role.PARENT);
        userRepo.save(user);
        return tokenPayload(user);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AuthUser auth) {
        User user = userRepo.findById(auth.getId()).orElseThrow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("name", user.getName());
        m.put("phone", user.getPhone());
        m.put("role", user.getRole().name());
        return m;
    }

    private Map<String, Object> tokenPayload(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", jwtUtil.generate(user));
        Map<String, Object> u = new LinkedHashMap<>();
        u.put("id", user.getId());
        u.put("username", user.getUsername());
        u.put("name", user.getName());
        u.put("role", user.getRole().name());
        m.put("user", u);
        return m;
    }
}
