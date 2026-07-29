package com.pozit.pozitserver.travel.service;

import org.springframework.data.domain.PageRequest;
import com.pozit.pozitserver.travel.domain.Region;
import com.pozit.pozitserver.travel.dto.response.RegionSearchScrollResponse;
import com.pozit.pozitserver.travel.dto.response.RegionSearchResponse;
import com.pozit.pozitserver.travel.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    private static final int SEARCH_LIMIT=10;

    /**
     * 여행 지역 검색
     * @param keyword
     * @return
     */
    public RegionSearchScrollResponse searchRegions(
            String keyword,
            int cursor,
            int size
    ){
        String trimmedKeyword=keyword==null?"":keyword.trim();

        if(trimmedKeyword.isEmpty()){
            return new RegionSearchScrollResponse(
                    cursor,
                    null,
                    false,
                    0,
                    List.of()
            );
        }

        Pageable pageable= PageRequest.of(cursor - 1,size + 1);
        List<RegionSearchResponse> regions = regionRepository.search(trimmedKeyword,pageable)
                .stream()
                .map(this::toResponse)
                .toList();

        boolean hasNext = regions.size() > size;
        List<RegionSearchResponse> currentRegions = hasNext
                ? regions.subList(0, size)
                : regions;

        return new RegionSearchScrollResponse(
                cursor,
                hasNext ? cursor + 1 : null,
                hasNext,
                currentRegions.size(),
                currentRegions
        );
    }

    private RegionSearchResponse toResponse(Region region){
        Region province=region.getParent();

        return new RegionSearchResponse(
                region.getCode(),
                region.getName(),
                province.getCode(),
                province.getName()
        );
    }


}
