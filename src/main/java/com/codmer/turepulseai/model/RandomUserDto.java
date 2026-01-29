package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified DTO for random user suggestions
 * Contains only username and name information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RandomUserDto {

    /**
     * Username of the user
     */
    private String userName;

    /**
     * First name of the user
     */
    private String firstName;

    /**
     * Last name of the user
     */
    private String lastName;
}

