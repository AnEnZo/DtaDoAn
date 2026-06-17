package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.ResetPasswordRequest;
import com.example.DtaAssigement.ennum.AuthProvider;
import com.example.DtaAssigement.ennum.UserStatus;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.payload.JwtResponse;
import com.example.DtaAssigement.payload.LoginRequest;
import com.example.DtaAssigement.payload.RegisterRequest;
import com.example.DtaAssigement.repository.UserRepository;
import com.example.DtaAssigement.security.JwtTokenUtil;
import com.example.DtaAssigement.service.EmailOtpService;
import com.example.DtaAssigement.service.SmsService;
import com.example.DtaAssigement.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private UserService userService;
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private JwtTokenUtil jwtTokenUtil;
    private final SmsService smsService;
    private final UserRepository userRepo;
    private final EmailOtpService emailOtpService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);
        if (user == null || user.getProvider() != AuthProvider.LOCAL) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Tên đăng nhập hoặc mật khẩu không chính xác");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tài khoản của bạn đã bị vô hiệu hóa");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails);

            return ResponseEntity.ok(new JwtResponse(token));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Tên đăng nhập hoặc mật khẩu không chính xác");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userService.existsByUsername(registerRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đăng nhập đã được sử dụng");
        }

        if (userService.existsByEmail(registerRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }

        userService.registerNewUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đăng ký thành công"));
    }

    @PostMapping("/forgot-password-email")
    public ResponseEntity<?> forgotPasswordEmail(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập là bắt buộc");
        }
        try {
            emailOtpService.createAndSendOtp(username);
            return ResponseEntity.ok(Map.of(
                    "message", "OTP đã được gửi đến email"));
        } catch (UsernameNotFoundException ex) {
            // không tiết lộ user có tồn tại hay không
            return ResponseEntity.ok(Map.of(
                    "message", "OTP đã được gửi đến email"));
        } catch (MailException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể gửi email OTP, vui lòng thử lại sau");
        }
    }

    @PostMapping("/reset-password-email")
    public ResponseEntity<?> resetPasswordEmail(@RequestBody ResetPasswordRequest req) {
        // UsernameNotFoundException -> 404, IllegalArgumentException -> 400 (handled globally)
        User user = emailOtpService.validateOtpAndGetUser(req.getUsername(), req.getOtp());
        userService.updatePassword(user, req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
    }

    @PostMapping("/forgot-password-sms")
    public ResponseEntity<?> forgotPasswordBySms(@RequestParam("username") String username) {
        userService.findByUsername(username).ifPresent(user -> {
            smsService.sendOtp(user.getPhoneNumber());
        });
        // Luôn trả về thông báo OTP (không tiết lộ user có tồn tại hay không)
        return ResponseEntity.ok(Map.of("message", "Đã gửi mã OTP đến số điện thoại."));
    }

    @PostMapping("/reset-password-sms")
    public ResponseEntity<?> resetPasswordBySms(@RequestParam("username") String username,
            @RequestParam("otp") String otp,
            @RequestParam("newPassword") String newPassword) {

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        if (!smsService.verifyOtp(user.getPhoneNumber(), otp)) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }

        // IllegalArgumentException from updatePassword -> 400 (handled globally)
        userService.updatePassword(user, newPassword);
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công!"));
    }

}
