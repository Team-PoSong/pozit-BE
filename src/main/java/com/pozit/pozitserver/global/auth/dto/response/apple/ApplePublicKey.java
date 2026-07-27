package com.pozit.pozitserver.global.auth.dto.response.apple;

public record ApplePublicKey (
        String kty,
        String kid,
        String use,
        String alg,
        String n,
        String e
){
}
