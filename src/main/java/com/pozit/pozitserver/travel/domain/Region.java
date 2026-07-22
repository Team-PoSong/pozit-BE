package com.pozit.pozitserver.travel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "regions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 행정구역 코드
     * 예: 47130
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * 시·도
     * 예: 경상북도
     */
    @Column(nullable = false, length = 30)
    private String sido;

    /**
     * 시·군·구
     * 예: 경주시
     */
    @Column(nullable = false, length = 30)
    private String sigungu;

    /**
     * 화면 표시용 이름
     * 예: 경상북도 경주시
     */
    @Column(nullable = false, length = 70)
    private String fullName;

    /**
     * 해당 지역을 여행 목적지로 노출할지 여부
     */
    @Column(nullable = false)
    private boolean active = true;

    private Double latitude;

    private Double longitude;
}