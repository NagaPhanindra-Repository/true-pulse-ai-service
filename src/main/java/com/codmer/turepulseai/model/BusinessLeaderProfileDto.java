package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessLeaderProfileDto {
    private Long entityId;
    private String fullName;
    private String company;
    private String projectName;
    private String projectDescription;
    private String mobileNumber;
    private String countryCode;
    private String email;
    private String contactHours;
}
