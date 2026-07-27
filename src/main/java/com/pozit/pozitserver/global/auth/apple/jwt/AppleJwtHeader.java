package com.pozit.pozitserver.global.auth.apple.jwt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppleJwtHeader (
        String kid,
        String alg
){
}
