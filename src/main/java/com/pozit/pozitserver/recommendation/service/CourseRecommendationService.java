package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.PlaceFeatureVector;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import com.pozit.pozitserver.recommendation.model.ScoredPlace;
import com.pozit.pozitserver.tag.repository.TravelTagRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseRecommendationService {

    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelTagRepository travelTagRepository;
    private final TourApiCandidateProvider candidateProvider;
    private final UserPreferenceVectorFactory userPreferenceVectorFactory;
    private final PlaceFeatureExtractor placeFeatureExtractor;
    private final ContentScoreCalculator contentScoreCalculator;
    private final ContextualRankingService contextualRankingService;
    private final DiversityRerankingService diversityRerankingService;
    private final RouteOptimizationService routeOptimizationService;

    public RecommendedCourseResponse preview(Long travelId, User currentUser) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));
        validateMember(travel, currentUser);

        CourseRecommendCommand command = toCommand(travel);
        List<CandidatePlace> candidates = candidateProvider.findCandidates(command);
        double[] userVector = userPreferenceVectorFactory.create(command.tags());

        List<ScoredPlace> scoredPlaces = candidates.stream()
                .map(place -> scorePlace(place, userVector, command))
                .toList();

        List<ScoredPlace> rankedPlaces = contextualRankingService.rank(scoredPlaces, command);
        List<ScoredPlace> diversifiedPlaces = diversityRerankingService.rerank(rankedPlaces);

        return routeOptimizationService.createCourse(diversifiedPlaces, command);
    }

    private ScoredPlace scorePlace(CandidatePlace place, double[] userVector, CourseRecommendCommand command) {
        PlaceFeatureVector featureVector = placeFeatureExtractor.extract(place);
        double contentScore = contentScoreCalculator.calculate(userVector, featureVector, command.tags());

        return ScoredPlace.contentOnly(place, featureVector, contentScore);
    }

    private CourseRecommendCommand toCommand(Travel travel) {
        List<RecommendationTag> tags = travelTagRepository.findTagNamesByTravelId(travel.getId()).stream()
                .map(RecommendationTag::fromName)
                .flatMap(Optional::stream)
                .distinct()
                .limit(2)
                .toList();

        if (tags.isEmpty()) {
            tags = List.of(RecommendationTag.RECORD, RecommendationTag.CULTURE);
        }

        return new CourseRecommendCommand(
                travel.getId(),
                travel.getDestination(),
                travel.getRegionCode(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getTravelStyle(),
                travel.getTransportation(),
                tags
        );
    }

    private void validateMember(Travel travel, User user) {
        if (!travelMemberRepository.existsByTravelAndUser(travel, user)) {
            throw new BusinessException(ErrorCode.NOT_VALID_TRAVEL_MEMBER);
        }
    }
}
