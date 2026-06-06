package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.OnCreate;
import com.example.DtaAssigement.dto.ResetPasswordRequest;
import com.example.DtaAssigement.dto.UserDTO;
import com.example.DtaAssigement.dto.UserUpdateDTO;
import com.example.DtaAssigement.security.CustomUserDetails;
import com.example.DtaAssigement.ennum.UserStatus;
import com.example.DtaAssigement.service.EmailOtpService;
import com.example.DtaAssigement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.*;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailOtpService emailOtpService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO dto = userService.getUserById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping("/user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO) {
        if (userService.existsByUsername(userDTO.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Username is already taken!");
        }

        if (userService.existsByEmail(userDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Email is already in use!");
        }
        UserDTO created = userService.createUser(userDTO);
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createStaff(@Validated(OnCreate.class) @RequestBody UserDTO userDTO) {
        if (userService.existsByUsername(userDTO.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Username is already taken!");
        }

        if (userService.existsByEmail(userDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Email is already in use!");
        }
        UserDTO created = userService.createStaff(userDTO);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO updateDTO) {
        try {
            UserDTO updated = userService.updateUserWithRole(id, updateDTO, updateDTO.getRole());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        boolean deactivated = userService.updateUserStatus(id, UserStatus.INACTIVE);
        return deactivated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        boolean updated = userService.updateUserStatus(id, status);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> searchUser(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.searchUser(keyword));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN','STAFF')")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal CustomUserDetails currentUser) {
        // Lấy username từ CustomUserDetails
        String username = currentUser.getUsername();
        UserDTO dto = userService.getUserByUsername(username);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN','STAFF')")
    public ResponseEntity<?> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, Object> updates) {
        try {
            String username = currentUser.getUsername();
            UserDTO updated = userService.updateCurrentUser(username, updates);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // Send OTP to current user's email to confirm sensitive updates (email/phone)
    @PostMapping("/me/otp/send")
    @PreAuthorize("hasAnyRole('USER','ADMIN','STAFF')")
    public ResponseEntity<?> sendOtpForCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String username = currentUser.getUsername();
            UserDTO current = userService.getUserByUsername(username);
            String newEmail = body != null ? (String) body.get("email") : null;
            String newPhone = body != null ? (String) body.get("phoneNumber") : null;

            boolean emailChanged = newEmail != null && !newEmail.equals(current.getEmail());
            boolean phoneChanged = newPhone != null && !newPhone.equals(current.getPhoneNumber());

            if (!emailChanged && !phoneChanged) {
                // No sensitive changes; do NOT send OTP
                return ResponseEntity.ok(Map.of("message", "Không cần OTP cho thay đổi này."));
            }

            emailOtpService.createAndSendOtp(username);
            return ResponseEntity.ok(Map.of("message", "OTP đã được gửi tới email."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getMessage());
        }
    }

    // Verify OTP and apply provided updates (email/phone)
    @PostMapping("/me/otp/verify-update")
    @PreAuthorize("hasAnyRole('USER','ADMIN','STAFF')")
    public ResponseEntity<?> verifyOtpAndUpdate(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, Object> body) {
        try {
            String username = currentUser.getUsername();
            String otp = (String) body.get("otp");
            if (otp == null || otp.isBlank()) {
                return ResponseEntity.badRequest().body("OTP là bắt buộc");
            }
            // Validate OTP -> returns User if valid
            var user = emailOtpService.validateOtpAndGetUser(username, otp);

            // Only allow specific fields
            Map<String, Object> updates = new java.util.HashMap<>();
            if (body.containsKey("email"))
                updates.put("email", body.get("email"));
            if (body.containsKey("phoneNumber"))
                updates.put("phoneNumber", body.get("phoneNumber"));

            // Apply updates
            UserDTO updated = userService.updateCurrentUser(username, updates);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException
                | org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * API tạo người dùng với role tùy chọn, truyền qua query param role=USER|STAFF
     * (mặc định USER)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUserByRole(
            @RequestParam(name = "role", defaultValue = "USER") String role,
            @Validated(OnCreate.class) @RequestBody UserDTO userDTO) {
        if (userService.existsByUsername(userDTO.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Username is already taken!");
        }

        if (userService.existsByEmail(userDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Email is already in use!");
        }
        try {
            UserDTO created = ((com.example.DtaAssigement.service.impl.UserServiceImpl) userService)
                    .createUserWithRole(userDTO, role);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasAnyRole('USER','ADMIN','STAFF')")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            // 1. Validate OTP và lấy User
            var user = emailOtpService.validateOtpAndGetUser(req.getUsername(), req.getOtp());

            // 2. Cập nhật password mới
            userService.updatePassword(user, req.getNewPassword());

            return ResponseEntity.ok("Password đã được cập nhật thành công");
        } catch (IllegalArgumentException
                | org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

}
