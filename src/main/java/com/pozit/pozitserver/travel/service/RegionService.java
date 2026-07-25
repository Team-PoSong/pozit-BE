package com.pozit.pozitserver.travel.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import com.pozit.pozitserver.travel.domain.Region;
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
    public List<RegionSearchResponse> searchRegions(String keyword){
        String trimmedKeyword=keyword==null?"":keyword.trim();

        if(trimmedKeyword.isEmpty()){
            return List.of();
        }

        Pageable pageable= PageRequest.of(0,SEARCH_LIMIT);
        return regionRepository.search(trimmedKeyword,pageable)
                .stream()
                .map(this::toResponse)
                .toList();
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
