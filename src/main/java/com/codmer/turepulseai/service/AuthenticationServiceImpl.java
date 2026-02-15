package com.codmer.turepulseai.service;

import com.codmer.turepulseai.entity.Role;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.CreateUserDto;
import com.codmer.turepulseai.model.LoginDto;
import com.codmer.turepulseai.repository.RoleRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.util.Gender;
import com.codmer.turepulseai.util.JwtTokenProvider;
import com.codmer.turepulseai.util.UserType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private RoleRepository roleRepository;


    public AuthenticationServiceImpl(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.roleRepository = roleRepository;
    }

    @Override
    public String login(LoginDto loginDto) {

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getUsernameOrEmail(), loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        return token;
    }

    @Override
    public User createUser(CreateUserDto createUserDto) {
        User user = new User();
        user.setUserName(createUserDto.getUserName());
        user.setCountryCode(createUserDto.getCountryCode());
        user.setEmail(createUserDto.getEmail());
        user.setPassword(passwordEncoder.encode(createUserDto.getPassword()));
        user.setFirstName(createUserDto.getFirstName());
        user.setLastName(createUserDto.getLastName());
        user.setMobileNumber(createUserDto.getMobileNumber());
        // default userType to MEMBER (CreateUserDto doesn't include userType yet)
        user.setUserType(UserType.MEMBER);

        try {
            user.setGender(Gender.valueOf(createUserDto.getGender()));
        } catch (Exception e) {
            user.setGender(null);
        }
        Set<Role> roles = new HashSet<>();
        Role role = roleRepository.findByName(UserType.USER.name())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + createUserDto.getRoleName()));
        roles.add(role);
        user.setRoles(roles);
        return userRepository.save(user);
    }
}
