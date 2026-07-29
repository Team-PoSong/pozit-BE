package com.pozit.pozitserver.travel.domain;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TravelStyle {

    RELAXED("여유롭게"),
    NORMAL("적당하게"),
    TIGHT("빡빡하게");

    private final String koreanName;
}
