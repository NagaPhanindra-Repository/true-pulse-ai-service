package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoliticianProfileDto {
    private Long entityId;
    private String fullName;
    private String partyName;
    private String segmentAddress;
    private String contestingTo;
    private String description;
    private String mobileNumber;
    private String countryCode;
    private String email;
    private String contactHours;
}
