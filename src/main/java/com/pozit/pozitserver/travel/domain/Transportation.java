package com.pozit.pozitserver.travel.domain;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Transportation {

    CAR("자동차"),
    WALK("도보"),
    PUBLIC("대중교통");

    private final String koreanName;
}