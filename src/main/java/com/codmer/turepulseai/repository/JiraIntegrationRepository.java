package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.JiraIntegration;
import com.codmer.turepulseai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JiraIntegrationRepository extends JpaRepository<JiraIntegration, UUID> {
    List<JiraIntegration> findByUser(User user);
    List<JiraIntegration> findByUserAndIsActiveTrue(User user);
    Optional<JiraIntegration> findByUserAndJiraUrl(User user, String jiraUrl);
    Optional<JiraIntegration> findByIdAndUser(UUID id, User user);
}

