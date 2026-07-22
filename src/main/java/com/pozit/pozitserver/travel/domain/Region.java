package com.pozit.pozitserver.travel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "regions",
        indexes = {
                @Index(
                        name = "idx_region_parent_code",
                        columnList = "parent_code"
                )
        }
)
@Getter
@Table(name = "regions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @Column(length=5)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="parent_code")
    private Region parent;
}