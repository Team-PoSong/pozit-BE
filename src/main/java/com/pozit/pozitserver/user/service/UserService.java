package com.pozit.pozitserver.user.service;

import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.dto.request.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    @Transactional
    public String makeNewNickname(
            User user,
            UserUpdateRequest request
    ){
        String nickname=request.nickname().trim();
        user.updateProfile(nickname);
        return nickname;
    }
}
