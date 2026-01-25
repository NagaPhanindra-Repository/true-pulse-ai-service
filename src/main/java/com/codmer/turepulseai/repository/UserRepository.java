package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);
    Boolean existsByEmail(String emailAddress);
    Optional<User> findByUserNameOrEmail(String userName, String emailAddress);

}