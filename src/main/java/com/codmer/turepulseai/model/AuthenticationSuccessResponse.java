package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationSuccessResponse {
    private String accessToken;
    private String tokenType = "Bearer";
}
