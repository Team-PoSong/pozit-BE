package com.pozit.pozitserver.global.auth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.apple")
public record AppleProperties(
        String issuer,
        String publicKeyUri,
        String bundleId,
        String serviceId,
        String teamId,
        String keyId,
        String privateKey
) {
}
