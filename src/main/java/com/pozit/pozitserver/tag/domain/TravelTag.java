package com.pozit.pozitserver.tag.domain;

import com.pozit.pozitserver.travel.domain.Travel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "travel_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_travel_tag",
                columnNames = {"travel_id", "tag_id"}
        )
)
public class TravelTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    private TravelTag(Travel travel, Tag tag) {
        this.travel = travel;
        this.tag = tag;
    }
}
