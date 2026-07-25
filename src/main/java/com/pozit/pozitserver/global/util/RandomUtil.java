package com.pozit.pozitserver.global.util;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomUtil {
    private static final String CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH=5;

    private static final SecureRandom secureRandom=new SecureRandom();

    @Autowired
    public static String generateInviteCode(){
        StringBuilder sb=new StringBuilder(CODE_LENGTH);
        for(int i=0;i<CODE_LENGTH;i++){
            sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

}
