package com.codmer.turepulseai.service;

import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.CreateUserDto;
import com.codmer.turepulseai.model.LoginDto;

import java.util.Optional;

public interface AuthenticationService {
    String login(LoginDto loginDto);
    User createUser(CreateUserDto createUserDto);
    Optional<User> extractUserFromCurrentRequestToken();
}