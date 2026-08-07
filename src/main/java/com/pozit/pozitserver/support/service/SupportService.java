package com.pozit.pozitserver.support.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.support.domain.Feedback;
import com.pozit.pozitserver.support.dto.request.FeedbackRequest;
import com.pozit.pozitserver.support.dto.response.SupportInfoResponse;
import com.pozit.pozitserver.support.dto.response.TermResponse;
import com.pozit.pozitserver.support.repository.FeedbackRepository;
import com.pozit.pozitserver.term.domain.Term;
import com.pozit.pozitserver.term.domain.TermType;
import com.pozit.pozitserver.term.repository.TermRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private final TermRepository termRepository;
    private final FeedbackRepository feedbackRepository;

    /**
     * 이용약관 및 개인정보처리방침의 최신 버전 조회
     */
    public SupportInfoResponse getSupportInfo() {
        return new SupportInfoResponse(
                getLatestTerm(TermType.SERVICE),
                getLatestTerm(TermType.PRIVACY)
        );
    }

    private TermResponse getLatestTerm(TermType type) {
        Term term = termRepository.findTopByTypeOrderByCreatedAtDesc(type)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERM_NOT_FOUND));
        return new TermResponse(term.getTitle(), term.getContent(), term.getVersion());
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
