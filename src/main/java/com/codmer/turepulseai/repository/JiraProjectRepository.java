package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.JiraIntegration;
import com.codmer.turepulseai.entity.JiraProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JiraProjectRepository extends JpaRepository<JiraProject, UUID> {

    /**
     * Find all projects for a Jira integration
     */
    List<JiraProject> findByJiraIntegration(JiraIntegration jiraIntegration);

    /**
     * Find project by integration and project key
     */
    JiraProject findByJiraIntegrationAndProjectKey(JiraIntegration jiraIntegration, String projectKey);
}

