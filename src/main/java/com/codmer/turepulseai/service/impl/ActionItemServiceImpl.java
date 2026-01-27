package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.ActionItemDto;
import com.codmer.turepulseai.entity.ActionItem;
import com.codmer.turepulseai.entity.Retro;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.repository.ActionItemRepository;
import com.codmer.turepulseai.repository.RetroRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.ActionItemService;
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
public class ActionItemServiceImpl implements ActionItemService {

    private final ActionItemRepository actionItemRepository;
    private final RetroRepository retroRepository;
    private final UserRepository userRepository;

    @Override
    public ActionItemDto create(ActionItemDto dto) {
        Retro retro = fetchRetro(dto.getRetroId());
        User assigned = dto.getAssignedUserId() != null ? fetchUser(dto.getAssignedUserId()) : null;
        ActionItem a = new ActionItem();
        a.setDescription(dto.getDescription());
        a.setDueDate(dto.getDueDate());
        a.setRetro(retro);
        a.setAssignedUser(assigned);
        // Set assigned user name if user is assigned
        if (assigned != null) {
            a.setAssignedUserName(assigned.getUserName());
        } else if (dto.getAssignedUserName() != null) {
            a.setAssignedUserName(dto.getAssignedUserName());
        }
        // Status and completed are set by @PrePersist
        return toDto(actionItemRepository.save(a));
    }

    @Override
    @Transactional(readOnly = true)
    public ActionItemDto getById(Long id) {
        return actionItemRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ActionItem not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActionItemDto> getAll() {
        return actionItemRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public ActionItemDto update(Long id, ActionItemDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        ActionItem a = actionItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ActionItem not found"));
        if (dto.getDescription() != null) a.setDescription(dto.getDescription());
        if (dto.getDueDate() != null) a.setDueDate(dto.getDueDate());
        if (dto.getRetroId() != null && (a.getRetro() == null || !dto.getRetroId().equals(a.getRetro().getId()))) {
            a.setRetro(fetchRetro(dto.getRetroId()));
        }
        if (dto.getAssignedUserId() != null && (a.getAssignedUser() == null || !dto.getAssignedUserId().equals(a.getAssignedUser().getId()))) {
            User assigned = fetchUser(dto.getAssignedUserId());
            a.setAssignedUser(assigned);
            a.setAssignedUserName(assigned.getUserName());
        }
        if (dto.getAssignedUserName() != null && dto.getAssignedUserId() == null) {
            a.setAssignedUserName(dto.getAssignedUserName());
        }
        a.setCompleted(dto.isCompleted());
        if (dto.getStatus() != null) a.setStatus(ActionItem.ActionItemStatus.valueOf(dto.getStatus()));
        if (dto.getCompletedAt() != null) a.setCompletedAt(dto.getCompletedAt());
        return toDto(actionItemRepository.save(a));
    }

    @Override
    public void delete(Long id) {
        if (!actionItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ActionItem not found");
        }
        actionItemRepository.deleteById(id);
    }

    @Override
    public List<ActionItemDto> getActionItemsByRetroId(Long retroId) {
        return actionItemRepository.findByRetroId(retroId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Retro fetchRetro(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "retroId is required");
        }
        return retroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retro not found"));
    }

    private User fetchUser(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private ActionItemDto toDto(ActionItem a) {
        Long retroId = a.getRetro() != null ? a.getRetro().getId() : null;
        Long userId = a.getAssignedUser() != null ? a.getAssignedUser().getId() : null;
        String userName = a.getAssignedUserName();
        return new ActionItemDto(
                a.getId(),
                a.getDescription(),
                a.getDueDate(),
                a.isCompleted(),
                a.getStatus().toString(),
                retroId,
                userId,
                userName,
                a.getCompletedAt(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}

