package com.example.DtaAssigement.entity;

import com.example.DtaAssigement.ennum.AuthProvider;
import com.example.DtaAssigement.ennum.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String displayName;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private Integer rewardPoints;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )

    @Builder.Default
    @JsonIgnore
    private Set<Roles> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private Set<UserVoucher> userVouchers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;   // LOCAL, GOOGLE, FACEBOOK…

    private String providerId;       // sẽ lưu “sub” hoặc “id” của user bên provider

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    public UserStatus getStatus() {
        return this.status == null ? UserStatus.ACTIVE : this.status;
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = UserStatus.ACTIVE;
    }
}
