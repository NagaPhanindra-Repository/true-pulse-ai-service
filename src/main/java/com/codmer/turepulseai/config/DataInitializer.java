package com.codmer.turepulseai.config;

import com.codmer.turepulseai.entity.Role;
import com.codmer.turepulseai.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        initializePgVector();
        initializeRoles();
    }

    private void initializePgVector() {
        try {
            log.info("Initializing pgvector extension...");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("pgvector extension initialized successfully");

            // Also try to add embedding column if it doesn't exist
            try {
                jdbcTemplate.execute("""
                        ALTER TABLE business_document_chunks
                        ADD COLUMN IF NOT EXISTS embedding vector
                        """);
                log.info("Embedding column added successfully");
            } catch (Exception e) {
                log.debug("Embedding column might already exist or vector type not available", e);
            }
        } catch (Exception e) {
            log.warn("""
                    Failed to initialize pgvector extension. 
                    IMPORTANT: pgvector is required for the document search feature.
                    Please install pgvector on your PostgreSQL instance:
                    
                    For Ubuntu/Debian:
                        sudo apt-get install postgresql-<version>-pgvector
                    
                    For macOS:
                        brew install pgvector
                    
                    For other systems, visit: https://github.com/pgvector/pgvector
                    
                    After installation, run: CREATE EXTENSION IF NOT EXISTS vector;
                    """, e);
        }
    }

    private void initializeRoles() {
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

