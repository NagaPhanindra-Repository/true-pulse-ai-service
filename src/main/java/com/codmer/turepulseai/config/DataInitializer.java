package com.codmer.turepulseai.config;

import com.codmer.turepulseai.entity.Role;
import com.codmer.turepulseai.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        createRoleIfNotExists("ADMIN", "Administrator");
        createRoleIfNotExists("LEADER", "Team leader");
        createRoleIfNotExists("MEMBER", "Team member");
    }

    private void createRoleIfNotExists(String name, String description) {
        roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDescription(description);
            return roleRepository.save(r);
        });
    }
}

