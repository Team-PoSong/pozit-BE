package com.pozit.pozitserver.travel.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.travel.domain.Region;
import com.pozit.pozitserver.travel.dto.response.RegionResponse;
import com.pozit.pozitserver.travel.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    public List<RegionResponse> getRegions() {
        return regionRepository
                .findAllByActiveTrueOrderBySidoAscSigunguAsc()
                .stream()
                .map(RegionResponse::from)
                .toList();
    }

    public Region getActiveRegion(Long regionId) {
        return regionRepository
                .findByIdAndActiveTrue(regionId)
                .orElseThrow(() ->
                    new BusinessException(ErrorCode.INVALID_REGION)
                );
    }
}
