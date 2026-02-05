package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfileDto {
    private Long entityId;
    private String fullName;
    private String address;
    private String description;
    private String businessType;
    private String mobileNumber;
    private String countryCode;
    private String email;
    private String contactHours;
}
