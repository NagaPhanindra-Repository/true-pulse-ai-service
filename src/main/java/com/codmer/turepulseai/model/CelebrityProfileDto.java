package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CelebrityProfileDto {
    private Long entityId;
    private String realName;
    private String artistName;
    private String artistType;
    private String description;
    private String mobileNumber;
    private String countryCode;
    private String email;
    private String contactHours;
}
