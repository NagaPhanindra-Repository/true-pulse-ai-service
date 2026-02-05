package com.codmer.turepulseai.model;

import com.codmer.turepulseai.entity.EntityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityCreateRequest {
    private EntityType type;
    private String displayName;
    private Long createdByUserId;

    // Business
    private String businessFullName;
    private String businessAddress;
    private String businessDescription;
    private String businessType;
    private String businessMobileNumber;
    private String businessCountryCode;
    private String businessEmail;
    private String businessContactHours;

    // Business Leader
    private String leaderFullName;
    private String leaderCompany;
    private String leaderProjectName;
    private String leaderProjectDescription;
    private String leaderMobileNumber;
    private String leaderCountryCode;
    private String leaderEmail;
    private String leaderContactHours;

    // Politician
    private String politicianFullName;
    private String politicianPartyName;
    private String politicianSegmentAddress;
    private String politicianContestingTo;
    private String politicianDescription;
    private String politicianMobileNumber;
    private String politicianCountryCode;
    private String politicianEmail;
    private String politicianContactHours;

    // Celebrity
    private String celebrityRealName;
    private String celebrityArtistName;
    private String celebrityArtistType;
    private String celebrityDescription;
    private String celebrityMobileNumber;
    private String celebrityCountryCode;
    private String celebrityEmail;
    private String celebrityContactHours;
}
