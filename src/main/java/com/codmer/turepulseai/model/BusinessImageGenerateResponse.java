package com.codmer.turepulseai.model;

import lombok.Data;
import lombok.Builder;
@Builder
@Data
public class BusinessImageGenerateResponse {
private String error;
private String displayName;
private Long entityId;
private String businessId;
private String revisedPrompt;
private String imageBase64;
private String mimeType;
private boolean success;
}





