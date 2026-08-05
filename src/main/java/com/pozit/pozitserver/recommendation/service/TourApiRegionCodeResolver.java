package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.TourApiRegionCodes;
import com.pozit.pozitserver.travel.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TourApiRegionCodeResolver {

    private static final Map<String, String> AREA_CODE_BY_LEGAL_REGION_CODE = Map.ofEntries(
            Map.entry("11", "1"),
            Map.entry("26", "6"),
            Map.entry("27", "4"),
            Map.entry("28", "2"),
            Map.entry("29", "5"),
            Map.entry("30", "3"),
            Map.entry("31", "7"),
            Map.entry("36", "8"),
            Map.entry("41", "31"),
            Map.entry("43", "33"),
            Map.entry("44", "34"),
            Map.entry("47", "35"),
            Map.entry("48", "36"),
            Map.entry("50", "39"),
            Map.entry("51", "32")
    );

    private final RegionRepository regionRepository;

    public TourApiRegionCodes resolve(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return new TourApiRegionCodes("",  "", "", "");
        }

        String normalizedCode = regionCode.trim();
        if (!regionRepository.existsByCode(normalizedCode)) {
            throw new BusinessException(ErrorCode.INVALID_REGION);
        }

        String legalDongRegionCode = normalizedCode.length() >= 2
                ? normalizedCode.substring(0, 2)
                : "";
        String legalDongSigunguCode = normalizedCode.length() >= 5 && !normalizedCode.endsWith("000")
                ? normalizedCode
                : "";
        String areaCode = AREA_CODE_BY_LEGAL_REGION_CODE.getOrDefault(legalDongRegionCode, "");

        return new TourApiRegionCodes(
                legalDongRegionCode,
                legalDongSigunguCode,
                areaCode,
                ""
        );
    }

    public boolean matches(CandidatePlace place, TourApiRegionCodes regionCodes) {
        if (regionCodes.legalDongSigunguCode() != null && !regionCodes.legalDongSigunguCode().isBlank()) {
            if (regionCodes.legalDongSigunguCode().equals(place.legalDongSigunguCode())) {
                return true;
            }
            return place.legalDongSigunguCode() == null
                    && isAreaCodeMatched(place, regionCodes);
        }

        if (regionCodes.legalDongRegionCode() != null && !regionCodes.legalDongRegionCode().isBlank()) {
            if (regionCodes.legalDongRegionCode().equals(place.legalDongRegionCode())) {
                return true;
            }
            return place.legalDongRegionCode() == null
                    && isAreaCodeMatched(place, regionCodes);
        }

        return true;
    }

    private boolean isAreaCodeMatched(CandidatePlace place, TourApiRegionCodes regionCodes) {
        return regionCodes.areaCode() != null
                && !regionCodes.areaCode().isBlank()
                && regionCodes.areaCode().equals(place.areaCode());
    }
}
