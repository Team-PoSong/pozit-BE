package com.pozit.pozitserver.test.service;

import com.pozit.pozitserver.test.domain.Sample;
import com.pozit.pozitserver.test.dto.SampleResponseDto;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    public SampleResponseDto getSample(){
        Sample sample=new Sample(
                "테스트 제목",
                "테스트 내용입니다."
        );
        return SampleResponseDto.from(sample);
    }

}
