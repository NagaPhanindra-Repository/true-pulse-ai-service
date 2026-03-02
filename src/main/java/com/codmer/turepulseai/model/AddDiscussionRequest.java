package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddDiscussionRequest {
    private String discussionText;
    private String decisionType;
    private String[] tags;
    private LocalDate meetingDate;
}

