package com.pozit.pozitserver.support.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.support.domain.Feedback;
import com.pozit.pozitserver.support.dto.request.FeedbackRequest;
import com.pozit.pozitserver.support.dto.response.SupportInfoResponse;
import com.pozit.pozitserver.support.dto.response.TermResponse;
import com.pozit.pozitserver.support.dto.response.TermSectionResponse;
import com.pozit.pozitserver.support.repository.FeedbackRepository;
import com.pozit.pozitserver.term.domain.Term;
import com.pozit.pozitserver.term.domain.TermType;
import com.pozit.pozitserver.term.repository.TermRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TermRepository termRepository;
    private final FeedbackRepository feedbackRepository;

    /**
     * 이용약관 및 개인정보처리방침의 최신 버전 조회
     */
    public SupportInfoResponse getSupportInfo() {
        return new SupportInfoResponse(
                getLatestTerm(TermType.SERVICE),
                getLatestTerm(TermType.PRIVACY),
                getLatestTerm(TermType.LOCATION)
        );
    }

    private TermResponse getLatestTerm(TermType type) {
        Term term = termRepository.findTopByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(type, LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.TERM_NOT_FOUND));
        return new TermResponse(
                term.getTitle(),
                term.getVersion(),
                term.getEffectiveDate(),
                parseSections(term.getContent())
        );
    }

    private List<TermSectionResponse> parseSections(String content) {
        try {
            return OBJECT_MAPPER.readValue(content, new TypeReference<List<TermSectionResponse>>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.COMMON500);
        }
    }

    @Transactional
    public void saveFeedback(User user, FeedbackRequest request) {
        Feedback feedback = Feedback.builder()
                .user(user)
                .content(request.content())
                .build();
        feedbackRepository.save(feedback);
    }
}
