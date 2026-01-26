package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.FeedbackPointDto;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.Retro;
import com.codmer.turepulseai.repository.FeedbackPointRepository;
import com.codmer.turepulseai.repository.RetroRepository;
import com.codmer.turepulseai.service.FeedbackPointService;
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
public class FeedbackPointServiceImpl implements FeedbackPointService {

    private final FeedbackPointRepository feedbackPointRepository;
    private final RetroRepository retroRepository;

    @Override
    public FeedbackPointDto create(FeedbackPointDto dto) {
        Retro retro = fetchRetro(dto.getRetroId());
        FeedbackPoint f = new FeedbackPoint();
        f.setType(FeedbackPoint.FeedbackType.valueOf(dto.getType()));
        f.setDescription(dto.getDescription());
        f.setRetro(retro);
        return toDto(feedbackPointRepository.save(f));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackPointDto getById(Long id) {
        return feedbackPointRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackPointDto> getAll() {
        return feedbackPointRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public FeedbackPointDto update(Long id, FeedbackPointDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        FeedbackPoint f = feedbackPointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found"));
        if (dto.getType() != null) f.setType(FeedbackPoint.FeedbackType.valueOf(dto.getType()));
        if (dto.getDescription() != null) f.setDescription(dto.getDescription());
        if (dto.getRetroId() != null && (f.getRetro() == null || !dto.getRetroId().equals(f.getRetro().getId()))) {
            f.setRetro(fetchRetro(dto.getRetroId()));
        }
        return toDto(feedbackPointRepository.save(f));
    }

    @Override
    public void delete(Long id) {
        if (!feedbackPointRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found");
        }
        feedbackPointRepository.deleteById(id);
    }

    private Retro fetchRetro(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "retroId is required");
        }
        return retroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retro not found"));
    }


    private FeedbackPointDto toDto(FeedbackPoint f) {
        return new FeedbackPointDto(
                f.getId(),
                f.getType().toString(),
                f.getDescription(),
                f.getRetro() != null ? f.getRetro().getId() : null,
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}

