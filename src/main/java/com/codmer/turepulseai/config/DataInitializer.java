package com.codmer.turepulseai.config;

import com.codmer.turepulseai.entity.Role;
import com.codmer.turepulseai.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        Map<String, String> roles = Map.of(
                "ADMIN", "Administrator",
                "USER", "User",
                "BUSINESS", "Business",
                "LEADER", "Team leader",
                "CELEBRITY", "Celebrity",
                "RESTAURANT", "Restaurant",
                "POLITICIAN", "Politician",
                "OTHER", "Other",
                "GUEST", "Guest"
        );
        roles.forEach(this::createRoleIfNotExists);
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

