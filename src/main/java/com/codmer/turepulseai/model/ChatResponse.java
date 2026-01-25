package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ChatResponse {
    String reply;
    String model;
    Long createdAt;
}
