package com.pozit.pozitserver.travel.repository;

import com.pozit.pozitserver.course.domain.QCourseSpot;
import com.pozit.pozitserver.course.domain.QTouristSpot;
import com.pozit.pozitserver.tag.domain.QTravelTag;
import com.pozit.pozitserver.travel.domain.QTravel;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class TravelRepositoryImpl implements TravelRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Travel> searchPublicTravels(
            String regionCode,
            LocalDate startDate,
            LocalDate endDate,
            List<Long> tagIds,
            String keyword
    ) {
        QTravel travel = QTravel.travel;

        return queryFactory
                .selectFrom(travel)
                .where(
                        travel.status.eq(TravelStatus.DONE),
                        travel.isPublic.isTrue(),
                        regionCodeCondition(regionCode),
                        dateRangeCondition(startDate, endDate),
                        tagsCondition(tagIds),
                        keywordCondition(keyword)
                )
                .orderBy(travel.endDate.desc(), travel.id.desc())
                .fetch();
    }

    // 지역 필터는 시/도 단위 선택이지만 Travel.regionCode에는 구 단위까지 세분화되어있을 경우
    private BooleanExpression regionCodeCondition(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return null;
        }
        return QTravel.travel.regionCode.startsWith(regionCode);
    }

    private BooleanExpression dateRangeCondition(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return QTravel.travel.startDate.goe(startDate).and(QTravel.travel.endDate.loe(endDate));
    }

    private BooleanExpression tagsCondition(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }

        List<Long> distinctTagIds = tagIds.stream().distinct().toList();
        QTravelTag travelTag = QTravelTag.travelTag;

        return QTravel.travel.id.in(
                JPAExpressions
                        .select(travelTag.travel.id)
                        .from(travelTag)
                        .where(travelTag.tag.id.in(distinctTagIds))
                        .groupBy(travelTag.travel.id)
                        .having(travelTag.tag.id.countDistinct().eq((long) distinctTagIds.size()))
        );
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        QTravel travel = QTravel.travel;

        return travel.title.contains(keyword)
                .or(travel.destination.contains(keyword))
                .or(travel.id.in(touristSpotNameMatchingTravelIds(keyword)));
    }

    private JPQLQuery<Long> touristSpotNameMatchingTravelIds(String keyword) {
        QCourseSpot courseSpot = QCourseSpot.courseSpot;
        QTouristSpot touristSpot = QTouristSpot.touristSpot;

        return JPAExpressions
                .select(courseSpot.course.travel.id)
                .from(courseSpot)
                .join(courseSpot.touristSpot, touristSpot)
                .where(touristSpot.name.contains(keyword));
    }
}
