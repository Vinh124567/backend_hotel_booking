package com.example.demo.service.user;

import com.example.demo.dto.user.UserRequestDto;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;


import lombok.AllArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;


    private final ModelMapper modelMapper;

    @Override
    public void createUser(UserRequestDto requestDto) {
        User user = modelMapper.map(requestDto, User.class);
        List<Role> roles = Optional.ofNullable(requestDto.getRoleIds())
                .filter(ids -> !ids.isEmpty())
                .map(roleRepository::findAllById)
                .orElse(Collections.emptyList());
        user.setRoles(roles);
        userRepository.save(user);
    }
}
