package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.RandomUserDto;
import com.codmer.turepulseai.model.UserDto;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.util.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        String username = jwtTokenProvider.getUsername(token);
        User user = userRepository.findByUserName(username).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setCountryCode(user.getCountryCode());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setGender(user.getGender() != null ? user.getGender().name() : null);
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setVerified(user.isVerified());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()));

        return ResponseEntity.ok(dto);
    }

    /**
     * Get 5 random users to suggest to the logged-in user
     * Returns different random users on each request
     *
     * Exclusions:
     * 1. Excludes the logged-in user from results
     * 2. Excludes users that logged-in user is already following
     *
     * Perfect for "People You May Know" or "Suggested Users" features
     *
     * GET /api/user/random
     * Authorization: Bearer <JWT_TOKEN>
     *
     * Response: List of RandomUserDto with username, firstName, lastName
     * (only users not being followed by logged-in user)
     */
    @GetMapping("/random")
    public ResponseEntity<List<RandomUserDto>> getRandomUsers(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {

        // Validate authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        // Extract and validate JWT token
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        // Get logged-in username
        String loggedInUsername = jwtTokenProvider.getUsername(token);

        // Verify user exists
        User loggedInUser = userRepository.findByUserName(loggedInUsername).orElse(null);
        if (loggedInUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Get 5 random users excluding:
        // 1. The logged-in user
        // 2. Users that logged-in user is already following
        List<User> randomUsers = userRepository.findRandomUsers(loggedInUsername, 5);

        // Convert to RandomUserDto
        List<RandomUserDto> randomUserDtos = randomUsers.stream()
                .map(user -> new RandomUserDto(
                        user.getUserName(),
                        user.getFirstName(),
                        user.getLastName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(randomUserDtos);
    }

}
