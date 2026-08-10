package com.pozit.pozitserver.global.util;

import java.security.SecureRandom;

public class RandomUtil {
    private static final String CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH=5;

    private static final SecureRandom secureRandom=new SecureRandom();

    public static String generateInviteCode(){
        StringBuilder sb=new StringBuilder(CODE_LENGTH);
        for(int i=0;i<CODE_LENGTH;i++){
            sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

}
