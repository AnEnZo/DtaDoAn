package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.dto.UserDTO;
import com.example.DtaAssigement.ennum.AuthProvider;
import com.example.DtaAssigement.ennum.UserStatus;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.mapper.UserMapper;
import com.example.DtaAssigement.payload.RegisterRequest;
import com.example.DtaAssigement.repository.RolesRepository;
import com.example.DtaAssigement.repository.UserRepository;
import com.example.DtaAssigement.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.DtaAssigement.entity.Roles;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RolesRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    @Override
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserMapper::toDTO);
    }

    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toDTO)
                .orElse(null);
    }

    public UserDTO createUser(UserDTO userDTO) {
        if (existsByUsername(userDTO.getUsername())) {
            throw new IllegalArgumentException("Username đã có người sử dụng");
        }
        if (existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email đã có người sử dụng");
        }
        // Mã hóa mật khẩu
        String encodedPassword = passwordEncoder.encode(userDTO.getPassword());
        Roles userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role USER không tồn tại"));

        User user = UserMapper.toEntity(userDTO);
        user.setRoles(Set.of(userRole));
        user.setPassword(encodedPassword);
        user.setRewardPoints(0);
        user.setProvider(AuthProvider.LOCAL);
        return UserMapper.toDTO(userRepository.save(user));
    }

    /**
     * Tạo user với role tùy chọn (USER hoặc STAFF)
     */
    public UserDTO createUserWithRole(UserDTO userDTO, String roleName) {
        if (existsByUsername(userDTO.getUsername())) {
            throw new IllegalArgumentException("Username đã có người sử dụng");
        }
        if (existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email đã có người sử dụng");
        }

        String normalized = (roleName == null ? "USER" : roleName).trim().toUpperCase();
        if (!normalized.equals("USER") && !normalized.equals("STAFF")) {
            throw new IllegalArgumentException("Role không hợp lệ. Chỉ chấp nhận USER hoặc STAFF");
        }

        Roles role = roleRepository.findByName("ROLE_" + normalized)
                .orElseThrow(() -> new RuntimeException("Error: Role " + normalized + " không tồn tại"));

        String encodedPassword = passwordEncoder.encode(userDTO.getPassword());

        User user = UserMapper.toEntity(userDTO);
        user.setRoles(Set.of(role));
        user.setPassword(encodedPassword);
        user.setRewardPoints(0);
        user.setProvider(AuthProvider.LOCAL);
        return UserMapper.toDTO(userRepository.save(user));
    }

    public UserDTO createStaff(UserDTO userDTO) {
        if (existsByUsername(userDTO.getUsername())) {
            throw new IllegalArgumentException("Username đã có người sử dụng");
        }
        if (existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email đã có người sử dụng");
        }
        Roles StaffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new RuntimeException("Error: Role STAFF không tồn tại"));

        // Mã hóa mật khẩu
        String encodedPassword = passwordEncoder.encode(userDTO.getPassword());

        User user = UserMapper.toEntity(userDTO);
        user.setRoles(Set.of(StaffRole));
        user.setPassword(encodedPassword);
        user.setProvider(AuthProvider.LOCAL);
        return UserMapper.toDTO(userRepository.save(user));
    }

    public UserDTO updateUser(Long id, UserDTO userDTO) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null)
            return null;

        // Mã hóa mật khẩu nếu có
        if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
            String encodedPassword = passwordEncoder.encode(userDTO.getPassword());
            user.setPassword(encodedPassword);
        }

        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());

        return UserMapper.toDTO(userRepository.save(user));
    }

    
    /**
     * Cập nhật user và (tuỳ chọn) cập nhật role USER/STAFF nếu roleName không null
     * và user hiện tại không phải ADMIN
     */
    public UserDTO updateUserWithRole(Long id, com.example.DtaAssigement.dto.UserUpdateDTO userDTO, String roleName) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null)
            return null;

        // cập nhật thông tin cơ bản
        user.setDisplayName(userDTO.getDisplayName());
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());

        // nếu user hiện tại có ADMIN thì không thay đổi role
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        if (!isAdmin && roleName != null && !roleName.isBlank()) {
            String normalized = roleName.trim().toUpperCase();
            if (!normalized.equals("USER") && !normalized.equals("STAFF")) {
                throw new IllegalArgumentException("Role không hợp lệ. Chỉ chấp nhận USER hoặc STAFF");
            }
            Roles role = roleRepository.findByName("ROLE_" + normalized)
                    .orElseThrow(() -> new RuntimeException("Error: Role " + normalized + " không tồn tại"));
            user.setRoles(new HashSet<>(Set.of(role)));
        }

        return UserMapper.toDTO(userRepository.save(user));
    }

    @Override
    public boolean updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null)
            return false;
        user.setStatus(status);
        userRepository.save(user);
        return true;
    }

    @Override
    public List<UserDTO> searchUser(String keyword) {
        return userRepository.findByUsernameContainingIgnoreCase(keyword).stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> searchUsersByPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().length() < 3) {
            return List.of();
        }
        return userRepository.findByPhoneNumberContaining(phoneNumber).stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public void registerNewUser(RegisterRequest registerRequest) {
        if (existsByUsername(registerRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username đã có người sử dụng");
        }
        if (existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email đã có người sử dụng");
        }
        // Mã hóa mật khẩu
        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
        Roles userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role USER không tồn tại"));

        User user = User.builder()
                .displayName(registerRequest.getDisplayName())
                .username(registerRequest.getUsername())
                .password(encodedPassword)
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .rewardPoints(0)
                .roles(Set.of(userRole)) // gán role mặc định
                .provider(AuthProvider.LOCAL)
                .build();

        // Lưu người dùng vào cơ sở dữ liệu
        userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void updatePassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User processOAuthUser(String provider, String providerId,
            String email, String username, String displayname) {
        Roles userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role USER không tồn tại"));

        Optional<User> optional = userRepository.findByEmail(email);
        if (optional.isPresent()) {
            User exist = optional.get();
            if (exist.getProvider() != AuthProvider.valueOf(provider.toUpperCase())) {
                throw new IllegalArgumentException(
                        "Looks like you're signed up with " + exist.getProvider());
            }
            // update info if cần
            return exist;
        } else {
            User u = User.builder()
                    .email(email)
                    .displayName(displayname)
                    .username(username)
                    .provider(AuthProvider.valueOf(provider.toUpperCase()))
                    .providerId(providerId)
                    .rewardPoints(0)
                    .roles(Set.of(userRole))
                    .build();
            return userRepository.save(u);
        }
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserMapper::toDTO)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public UserDTO updateCurrentUser(String username, Map<String, Object> updates) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Cập nhật từng trường nếu được truyền
        if (updates.containsKey("displayName")) {
            String dn = (String) updates.get("displayName");
            if (dn == null || dn.isBlank())
                throw new IllegalArgumentException("displayName không được để trống");
            user.setDisplayName(dn);
        }

        if (updates.containsKey("email")) {
            String email = (String) updates.get("email");
            if (!email.matches("^[\\w-.]+@[\\w-]+(\\.[\\w-]+)+$"))
                throw new IllegalArgumentException("Email không hợp lệ");
            if (!email.equals(user.getEmail()) && existsByEmail(email))
                throw new IllegalArgumentException("Email đã được sử dụng");
            user.setEmail(email);
        }

        if (updates.containsKey("phoneNumber")) {
            String phone = (String) updates.get("phoneNumber");
            if (!phone.matches("^[0-9]{10}$"))
                throw new IllegalArgumentException("SĐT phải là 10 chữ số");
            user.setPhoneNumber(phone);
        }

        User saved = userRepository.save(user);
        return UserMapper.toDTO(saved);
    }

}
