package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.DiscussionDto;
import com.codmer.turepulseai.entity.Discussion;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.repository.DiscussionRepository;
import com.codmer.turepulseai.repository.FeedbackPointRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.DiscussionService;
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
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final FeedbackPointRepository feedbackPointRepository;
    private final UserRepository userRepository;

    @Override
    public DiscussionDto create(DiscussionDto dto) {
        FeedbackPoint feedbackPoint = fetchFeedbackPoint(dto.getFeedbackPointId());
        User user = fetchUser(dto.getUserId());
        Discussion d = new Discussion();
        d.setNote(dto.getNote());
        d.setFeedbackPoint(feedbackPoint);
        d.setUser(user);
        return toDto(discussionRepository.save(d));
    }

    @Override
    @Transactional(readOnly = true)
    public DiscussionDto getById(Long id) {
        return discussionRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discussion not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionDto> getAll() {
        return discussionRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public DiscussionDto update(Long id, DiscussionDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        Discussion d = discussionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discussion not found"));
        if (dto.getNote() != null) d.setNote(dto.getNote());
        if (dto.getFeedbackPointId() != null && (d.getFeedbackPoint() == null || !dto.getFeedbackPointId().equals(d.getFeedbackPoint().getId()))) {
            d.setFeedbackPoint(fetchFeedbackPoint(dto.getFeedbackPointId()));
        }
        if (dto.getUserId() != null && (d.getUser() == null || !dto.getUserId().equals(d.getUser().getId()))) {
            d.setUser(fetchUser(dto.getUserId()));
        }
        return toDto(discussionRepository.save(d));
    }

    @Override
    public void delete(Long id) {
        if (!discussionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Discussion not found");
        }
        discussionRepository.deleteById(id);
    }

    private FeedbackPoint fetchFeedbackPoint(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "feedbackPointId is required");
        }
        return feedbackPointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "FeedbackPoint not found"));
    }

    private User fetchUser(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private DiscussionDto toDto(Discussion d) {
        Long feedbackPointId = d.getFeedbackPoint() != null ? d.getFeedbackPoint().getId() : null;
        Long userId = d.getUser() != null ? d.getUser().getId() : null;
        return new DiscussionDto(d.getId(), d.getNote(), feedbackPointId, userId, d.getCreatedAt(), d.getUpdatedAt());
    }
}

