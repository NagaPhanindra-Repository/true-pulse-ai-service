package com.codmer.turepulseai.controller;


import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.AuthenticationSuccessResponse;
import com.codmer.turepulseai.model.CreateUserDto;
import com.codmer.turepulseai.model.LoginDto;
import com.codmer.turepulseai.service.AuthenticationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
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
