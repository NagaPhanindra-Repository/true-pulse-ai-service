package com.codmer.turepulseai.service;

import com.codmer.turepulseai.entity.*;
import com.codmer.turepulseai.model.*;
import com.codmer.turepulseai.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class FeatureMemoryService {

    private final FeatureMemoryRepository memoryRepository;
    private final MemoryDiscussionRepository discussionRepository;
    private final GitBranchMappingRepository branchRepository;
    private final MemoryAttachmentRepository attachmentRepository;
    private final JiraIntegrationRepository jiraIntegrationRepository;

    public FeatureMemoryService(
        FeatureMemoryRepository memoryRepository,
        MemoryDiscussionRepository discussionRepository,
        GitBranchMappingRepository branchRepository,
        MemoryAttachmentRepository attachmentRepository,
        JiraIntegrationRepository jiraIntegrationRepository) {
        this.memoryRepository = memoryRepository;
        this.discussionRepository = discussionRepository;
        this.branchRepository = branchRepository;
        this.attachmentRepository = attachmentRepository;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
    }

    /**
     * Create a new feature memory
     */
    public FeatureMemory createMemory(CreateMemoryRequest request, User user, JiraStoryDto jiraStory) {
        Optional<FeatureMemory> existing = memoryRepository.findByUserAndJiraStoryKey(user, request.getJiraStoryKey());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Feature memory already exists for story: " + request.getJiraStoryKey());
        }

        JiraIntegration integration = null;
        if (request.getJiraIntegrationId() != null) {
            integration = jiraIntegrationRepository.findByIdAndUser(request.getJiraIntegrationId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Jira integration not found"));
        }

        FeatureMemory memory = FeatureMemory.builder()
            .user(user)
            .jiraIntegration(integration)
            .jiraStoryKey(request.getJiraStoryKey())
            .jiraStoryId(jiraStory != null ? jiraStory.getId() : null)
            .jiraStoryTitle(jiraStory != null ? jiraStory.getSummary() : null)
            .jiraStoryDescription(jiraStory != null ? jiraStory.getDescription() : null)
            .jiraStoryType(jiraStory != null ? jiraStory.getIssueType() : null)
            .jiraAssignee(jiraStory != null ? jiraStory.getAssignee() : null)
            .jiraStatus(jiraStory != null ? jiraStory.getStatus() : null)
            .initialDescription(request.getInitialDescription())
            .status("active")
            .build();

        FeatureMemory saved = memoryRepository.save(memory);

        if (request.getBranchName() != null && !request.getBranchName().isEmpty()) {
            linkGitBranch(saved, request.getBranchName(), null);
        }

        return saved;
    }

    /**
     * Get paginated memories for a user
     */
    @Transactional(readOnly = true)
    public Page<FeatureMemoryDto> getUserMemories(User user, String status, String search, Pageable pageable) {
        Page<FeatureMemory> memories;

        if (search != null && !search.isEmpty()) {
            memories = memoryRepository.searchByUser(user, search, pageable);
        } else if (status != null && !status.isEmpty()) {
            memories = memoryRepository.findByUserAndStatus(user, status, pageable);
        } else {
            memories = memoryRepository.findByUser(user, pageable);
        }

        return memories.map(this::convertToDto);
    }

    /**
     * Get detailed memory with all discussions and branches
     */
    @Transactional(readOnly = true)
    public FeatureMemoryDetailDto getMemoryDetail(UUID memoryId, User user) {
        FeatureMemory memory = memoryRepository.findByIdAndUser(memoryId, user)
            .orElseThrow(() -> new IllegalArgumentException("Feature memory not found"));

        List<MemoryDiscussion> discussions = discussionRepository.findByFeatureMemoryOrderByRecordedAtAsc(memory);
        List<GitBranchMapping> branches = branchRepository.findByFeatureMemory(memory);

        return FeatureMemoryDetailDto.builder()
            .id(memory.getId())
            .jiraStoryKey(memory.getJiraStoryKey())
            .jiraStoryTitle(memory.getJiraStoryTitle())
            .jiraStoryDescription(memory.getJiraStoryDescription())
            .jiraStoryType(memory.getJiraStoryType())
            .jiraAssignee(memory.getJiraAssignee())
            .jiraStatus(memory.getJiraStatus())
            .initialDescription(memory.getInitialDescription())
            .status(memory.getStatus())
            .createdAt(memory.getCreatedAt())
            .updatedAt(memory.getUpdatedAt())
            .completedAt(memory.getCompletedAt())
            .discussions(discussions.stream().map(this::convertDiscussionToDto).collect(Collectors.toList()))
            .branches(branches.stream().map(this::convertBranchToDto).collect(Collectors.toList()))
            .build();
    }

    /**
     * Update memory
     */
    public FeatureMemory updateMemory(UUID memoryId, User user, FeatureMemory updates) {
        FeatureMemory memory = memoryRepository.findByIdAndUser(memoryId, user)
            .orElseThrow(() -> new IllegalArgumentException("Feature memory not found"));

        if (updates.getInitialDescription() != null) {
            memory.setInitialDescription(updates.getInitialDescription());
        }
        if (updates.getStatus() != null) {
            memory.setStatus(updates.getStatus());
        }

        return memoryRepository.save(memory);
    }

    /**
     * Complete a memory
     */
    public void completeMemory(UUID memoryId, User user) {
        FeatureMemory memory = memoryRepository.findByIdAndUser(memoryId, user)
            .orElseThrow(() -> new IllegalArgumentException("Feature memory not found"));

        memory.setStatus("completed");
        memory.setCompletedAt(LocalDateTime.now());
        memoryRepository.save(memory);
    }

    /**
     * Archive a memory
     */
    public void archiveMemory(UUID memoryId, User user) {
        FeatureMemory memory = memoryRepository.findByIdAndUser(memoryId, user)
            .orElseThrow(() -> new IllegalArgumentException("Feature memory not found"));

        memory.setStatus("archived");
        memoryRepository.save(memory);
    }

    /**
     * Delete a memory
     */
    public void deleteMemory(UUID memoryId, User user) {
        FeatureMemory memory = memoryRepository.findByIdAndUser(memoryId, user)
            .orElseThrow(() -> new IllegalArgumentException("Feature memory not found"));

        memoryRepository.delete(memory);
    }

    /**
     * Link a git branch to a feature memory
     */
    public GitBranchMapping linkGitBranch(FeatureMemory memory, String branchName, String repositoryUrl) {
        Optional<GitBranchMapping> existing = branchRepository.findByFeatureMemoryAndBranchName(memory, branchName);
        if (existing.isPresent()) {
            return existing.get();
        }

        GitBranchMapping mapping = GitBranchMapping.builder()
            .featureMemory(memory)
            .branchName(branchName)
            .repositoryUrl(repositoryUrl)
            .build();

        return branchRepository.save(mapping);
    }

    /**
     * Find feature memory by branch name
     */
    @Transactional(readOnly = true)
    public Optional<FeatureMemory> findByBranchName(String branchName) {
        return branchRepository.findByBranchName(branchName)
            .map(GitBranchMapping::getFeatureMemory);
    }

    /**
     * Extract story key from branch name
     */
    public Optional<String> extractStoryKeyFromBranch(String branchName) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Z]+-\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(branchName);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * Add a discussion note
     */
    public MemoryDiscussion addDiscussion(UUID memoryId, User user, AddDiscussionRequest request) {
        FeatureMemory memory = memoryRepository.findByIdAndUser(memoryId, user)
            .orElseThrow(() -> new IllegalArgumentException("Feature memory not found"));

        MemoryDiscussion discussion = MemoryDiscussion.builder()
            .featureMemory(memory)
            .user(user)
            .discussionText(request.getDiscussionText())
            .decisionType(request.getDecisionType())
            .tags(request.getTags())
            .meetingDate(request.getMeetingDate())
            .build();

        return discussionRepository.save(discussion);
    }

    /**
     * Get discussions for a memory, optionally filtered by type
     */
    @Transactional(readOnly = true)
    public List<MemoryDiscussionDto> getDiscussions(UUID memoryId, User user, String type) {
        FeatureMemory memory = memoryRepository.findByIdAndUser(memoryId, user)
            .orElseThrow(() -> new IllegalArgumentException("Feature memory not found"));

        List<MemoryDiscussion> discussions;
        if (type != null && !type.isEmpty()) {
            discussions = discussionRepository.findByFeatureMemoryAndDecisionTypeOrderByRecordedAtAsc(memory, type);
        } else {
            discussions = discussionRepository.findByFeatureMemoryOrderByRecordedAtAsc(memory);
        }

        return discussions.stream()
            .map(this::convertDiscussionToDto)
            .collect(Collectors.toList());
    }

    /**
     * Update a discussion
     */
    public MemoryDiscussion updateDiscussion(UUID discussionId, User user, MemoryDiscussion updates) {
        MemoryDiscussion discussion = discussionRepository.findById(discussionId)
            .orElseThrow(() -> new IllegalArgumentException("Discussion not found"));

        if (!discussion.getFeatureMemory().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (updates.getDiscussionText() != null) {
            discussion.setDiscussionText(updates.getDiscussionText());
        }
        if (updates.getDecisionType() != null) {
            discussion.setDecisionType(updates.getDecisionType());
        }
        if (updates.getTags() != null) {
            discussion.setTags(updates.getTags());
        }

        return discussionRepository.save(discussion);
    }

    /**
     * Delete a discussion
     */
    public void deleteDiscussion(UUID discussionId, User user) {
        MemoryDiscussion discussion = discussionRepository.findById(discussionId)
            .orElseThrow(() -> new IllegalArgumentException("Discussion not found"));

        if (!discussion.getFeatureMemory().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        discussionRepository.delete(discussion);
    }

    // ==================== DTO Conversion Methods ====================

    private FeatureMemoryDto convertToDto(FeatureMemory memory) {
        long discussionCount = discussionRepository.countByFeatureMemory(memory);

        return FeatureMemoryDto.builder()
            .id(memory.getId())
            .jiraStoryKey(memory.getJiraStoryKey())
            .jiraStoryTitle(memory.getJiraStoryTitle())
            .jiraStoryDescription(memory.getJiraStoryDescription())
            .status(memory.getStatus())
            .discussionCount(discussionCount)
            .createdAt(memory.getCreatedAt())
            .updatedAt(memory.getUpdatedAt())
            .build();
    }

    private MemoryDiscussionDto convertDiscussionToDto(MemoryDiscussion discussion) {
        List<MemoryAttachment> attachments = attachmentRepository.findByDiscussion(discussion);

        return MemoryDiscussionDto.builder()
            .id(discussion.getId())
            .discussionText(discussion.getDiscussionText())
            .decisionType(discussion.getDecisionType())
            .tags(discussion.getTags())
            .meetingDate(discussion.getMeetingDate())
            .recordedAt(discussion.getRecordedAt())
            .updatedAt(discussion.getUpdatedAt())
            .authorName(discussion.getUser().getFirstName() + " " + discussion.getUser().getLastName())
            .attachments(attachments.stream().map(this::convertAttachmentToDto).collect(Collectors.toList()))
            .build();
    }

    private MemoryAttachmentDto convertAttachmentToDto(MemoryAttachment attachment) {
        return MemoryAttachmentDto.builder()
            .id(attachment.getId())
            .fileName(attachment.getFileName())
            .fileUrl(attachment.getFileUrl())
            .fileType(attachment.getFileType())
            .fileSizeBytes(attachment.getFileSizeBytes())
            .uploadedAt(attachment.getUploadedAt())
            .build();
    }

    private GitBranchMappingDto convertBranchToDto(GitBranchMapping branch) {
        return GitBranchMappingDto.builder()
            .id(branch.getId())
            .branchName(branch.getBranchName())
            .repositoryUrl(branch.getRepositoryUrl())
            .createdAt(branch.getCreatedAt())
            .build();
    }
}

