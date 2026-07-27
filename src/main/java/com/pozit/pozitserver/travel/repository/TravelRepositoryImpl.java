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

    private static final String NATIONWIDE_REGION = "전국";

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Travel> searchPublicTravels(
            String region,
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
                        regionCondition(region),
                        dateRangeCondition(startDate, endDate),
                        tagsCondition(tagIds),
                        keywordCondition(keyword)
                )
                .orderBy(travel.endDate.desc(), travel.id.desc())
                .fetch();
    }

    private BooleanExpression regionCondition(String region) {
        if (region == null || region.isBlank() || region.equals(NATIONWIDE_REGION)) {
            return null;
        }
        return QTravel.travel.destination.contains(region);
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

        QTravelTag travelTag = QTravelTag.travelTag;

        return QTravel.travel.id.in(
                JPAExpressions
                        .select(travelTag.travel.id)
                        .from(travelTag)
                        .where(travelTag.tag.id.in(tagIds))
                        .groupBy(travelTag.travel.id)
                        .having(travelTag.tag.id.countDistinct().eq((long) tagIds.size()))
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
