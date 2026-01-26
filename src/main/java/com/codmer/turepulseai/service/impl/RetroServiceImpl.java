package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.RetroDto;
import com.codmer.turepulseai.model.RetroDetailDto;
import com.codmer.turepulseai.entity.Retro;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.Discussion;
import com.codmer.turepulseai.entity.ActionItem;
import com.codmer.turepulseai.repository.RetroRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.RetroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RetroServiceImpl implements RetroService {

    private final RetroRepository retroRepository;
    private final UserRepository userRepository;


    @Override
    @Transactional(readOnly = true)
    public RetroDetailDto getRetroDetails(Long retroId) {
        Retro retro = retroRepository.findById(retroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found"));

        return toDetailDto(retro);
    }

    private RetroDetailDto toDetailDto(Retro retro) {
        RetroDetailDto dto = new RetroDetailDto();
        dto.setId(retro.getId());
        dto.setTitle(retro.getTitle());
        dto.setDescription(retro.getDescription());
        dto.setUserId(retro.getUser() != null ? retro.getUser().getId() : null);
        dto.setCreatedBy(retro.getUser() != null ? retro.getUser().getUserName() : null);
        dto.setCreatedAt(retro.getCreatedAt());
        dto.setUpdatedAt(retro.getUpdatedAt());

        // Map Feedback Points with nested Discussions
        if (retro.getFeedbackPoints() != null && !retro.getFeedbackPoints().isEmpty()) {
            List<RetroDetailDto.FeedbackPointDetailDto> feedbackDtos = retro.getFeedbackPoints()
                    .stream()
                    .map(this::toFeedbackPointDetailDto)
                    .collect(Collectors.toList());
            dto.setFeedbackPoints(feedbackDtos);
        } else {
            dto.setFeedbackPoints(new java.util.ArrayList<>());
        }

        // Map Action Items with assignee details
        if (retro.getActionItems() != null && !retro.getActionItems().isEmpty()) {
            List<RetroDetailDto.ActionItemDetailDto> actionItemDtos = retro.getActionItems()
                    .stream()
                    .map(this::toActionItemDetailDto)
                    .collect(Collectors.toList());
            dto.setActionItems(actionItemDtos);
        } else {
            dto.setActionItems(new java.util.ArrayList<>());
        }


        return dto;
    }

    private RetroDetailDto.FeedbackPointDetailDto toFeedbackPointDetailDto(FeedbackPoint fp) {
        RetroDetailDto.FeedbackPointDetailDto dto = new RetroDetailDto.FeedbackPointDetailDto();
        dto.setId(fp.getId());
        dto.setType(fp.getType().toString());
        dto.setDescription(fp.getDescription());
        dto.setCreatedAt(fp.getCreatedAt());
        dto.setUpdatedAt(fp.getUpdatedAt());

        // Map nested discussions with user details
        if (fp.getDiscussions() != null && !fp.getDiscussions().isEmpty()) {
            List<RetroDetailDto.DiscussionDetailDto> discussionDtos = fp.getDiscussions()
                    .stream()
                    .map(this::toDiscussionDetailDto)
                    .collect(Collectors.toList());
            dto.setDiscussions(discussionDtos);
        } else {
            dto.setDiscussions(new java.util.ArrayList<>());
        }

        return dto;
    }

    private RetroDetailDto.DiscussionDetailDto toDiscussionDetailDto(Discussion discussion) {
        RetroDetailDto.DiscussionDetailDto dto = new RetroDetailDto.DiscussionDetailDto();
        dto.setId(discussion.getId());
        dto.setNote(discussion.getNote());

        if (discussion.getUser() != null) {
            dto.setUserId(discussion.getUser().getId());
            dto.setUserName(discussion.getUser().getUserName());
            dto.setUserFirstName(discussion.getUser().getFirstName());
            dto.setUserLastName(discussion.getUser().getLastName());
        }

        dto.setCreatedAt(discussion.getCreatedAt());
        dto.setUpdatedAt(discussion.getUpdatedAt());

        return dto;
    }

    private RetroDetailDto.ActionItemDetailDto toActionItemDetailDto(ActionItem actionItem) {
        RetroDetailDto.ActionItemDetailDto dto = new RetroDetailDto.ActionItemDetailDto();
        dto.setId(actionItem.getId());
        dto.setDescription(actionItem.getDescription());
        dto.setDueDate(actionItem.getDueDate());
        dto.setCompleted(actionItem.isCompleted());
        dto.setStatus(actionItem.getStatus().toString());
        dto.setCompletedAt(actionItem.getCompletedAt());

        if (actionItem.getAssignedUser() != null) {
            dto.setAssignedUserId(actionItem.getAssignedUser().getId());
            dto.setAssignedUserName(actionItem.getAssignedUser().getUserName());
            dto.setAssignedUserFirstName(actionItem.getAssignedUser().getFirstName());
            dto.setAssignedUserLastName(actionItem.getAssignedUser().getLastName());
        }

        dto.setCreatedAt(actionItem.getCreatedAt());
        dto.setUpdatedAt(actionItem.getUpdatedAt());

        return dto;
    }


    @Override
    public RetroDto create(RetroDto dto) {
        User user = fetchUser(dto.getUserId());
        Retro r = new Retro();
        r.setTitle(dto.getTitle());
        r.setDescription(dto.getDescription());
        r.setUser(user);
        Retro saved = retroRepository.save(r);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RetroDto getById(Long id) {
        return retroRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetroDto> getAll() {
        return retroRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public RetroDto update(Long id, RetroDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        Retro r = retroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found"));
        if (dto.getTitle() != null) r.setTitle(dto.getTitle());
        if (dto.getDescription() != null) r.setDescription(dto.getDescription());
        if (dto.getUserId() != null && (r.getUser() == null || !dto.getUserId().equals(r.getUser().getId()))) {
            r.setUser(fetchUser(dto.getUserId()));
        }
        return toDto(retroRepository.save(r));
    }

    @Override
    public void delete(Long id) {
        if (!retroRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found");
        }
        retroRepository.deleteById(id);
    }

    private User fetchUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private RetroDto toDto(Retro r) {
        Long userId = r.getUser() != null ? r.getUser().getId() : null;
        return new RetroDto(r.getUpdatedAt(), r.getCreatedAt(), userId, r.getDescription(), r.getTitle(), r.getId());
    }
}

