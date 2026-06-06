package com.example.DtaAssigement.mapper;


import com.example.DtaAssigement.dto.UserDTO;
import com.example.DtaAssigement.entity.Roles;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.ennum.UserStatus;

import java.util.stream.Collectors;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .displayName(user.getDisplayName())
                .rewardPoints(user.getRewardPoints())
                .roles(user.getRoles().stream()
                        .map(Roles::getName)
                        .collect(Collectors.toSet()))
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .providerId(user.getProviderId())
                .status(user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name())
                .build();
    }

    public static User toEntity(UserDTO dto) {
        return User.builder()
                .id(dto.getId())
                .username(dto.getUsername())
                .password(dto.getPassword())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .displayName(dto.getDisplayName())
                .status(dto.getStatus() != null ? UserStatus.valueOf(dto.getStatus()) : UserStatus.ACTIVE)
                .build();
    }
}
