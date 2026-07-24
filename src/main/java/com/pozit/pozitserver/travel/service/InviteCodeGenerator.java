package com.pozit.pozitserver.travel.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InviteCodeGenerator {
    private static final String CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH=8;

    private final SecureRandom secureRandom=new SecureRandom();
}
