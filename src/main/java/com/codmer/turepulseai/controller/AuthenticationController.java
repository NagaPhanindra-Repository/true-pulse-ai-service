package com.codmer.turepulseai.controller;


import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.AuthenticationSuccessResponse;
import com.codmer.turepulseai.model.CreateUserDto;
import com.codmer.turepulseai.model.LoginDto;
import com.codmer.turepulseai.model.UserDto;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.AuthenticationService;
import com.codmer.turepulseai.util.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationSuccessResponse> authenticate(@RequestBody LoginDto loginDto){
        String token = authenticationService.login(loginDto);
        AuthenticationSuccessResponse jwtAuthResponse = new AuthenticationSuccessResponse();
        jwtAuthResponse.setAccessToken(token);
        return ResponseEntity.ok(jwtAuthResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signIn(@RequestBody CreateUserDto createUserDto){
        User user = authenticationService.createUser(createUserDto);

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUserName(user.getUserName());
        userDto.setEmail(user.getEmail());
        userDto.setCountryCode(user.getCountryCode());
        userDto.setMobileNumber(user.getMobileNumber());
        userDto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setGender(user.getGender() != null ? user.getGender().name() : null);
        userDto.setDateOfBirth(user.getDateOfBirth());
        userDto.setVerified(user.isVerified());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());
        userDto.setRoles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()));

        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUserNameOrEmail(username, username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUserName(user.getUserName());
        userDto.setEmail(user.getEmail());
        userDto.setCountryCode(user.getCountryCode());
        userDto.setMobileNumber(user.getMobileNumber());
        userDto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setGender(user.getGender() != null ? user.getGender().name() : null);
        userDto.setDateOfBirth(user.getDateOfBirth());
        userDto.setVerified(user.isVerified());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());
        userDto.setRoles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()));

        return ResponseEntity.ok(userDto);
    }
}
