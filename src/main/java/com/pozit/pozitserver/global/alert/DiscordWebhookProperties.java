package com.pozit.pozitserver.global.alert;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "discord.webhook")
public class DiscordWebhookProperties {

    private boolean enabled = false;
    private String url;
    private String username = "POZIT Server";

    public boolean isAvailable() {
        return enabled && StringUtils.hasText(url);
    }
}
