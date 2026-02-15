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
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationSuccessResponse> authenticate(@RequestBody LoginDto loginDto){
        String token = authenticationService.login(loginDto);
        AuthenticationSuccessResponse jwtAuthResponse = new AuthenticationSuccessResponse();
        jwtAuthResponse.setAccessToken(token);
        return ResponseEntity.ok(jwtAuthResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<User> signIn(@RequestBody CreateUserDto createUserDto){
        User token = authenticationService.createUser(createUserDto);
        return ResponseEntity.ok(token);
    }
}
