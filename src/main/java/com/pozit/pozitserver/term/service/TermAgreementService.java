package com.pozit.pozitserver.term.service;

import com.pozit.pozitserver.term.domain.Term;
import com.pozit.pozitserver.term.domain.TermAgreement;
import com.pozit.pozitserver.term.domain.TermType;
import com.pozit.pozitserver.term.dto.request.TermAgreementItemRequest;
import com.pozit.pozitserver.term.dto.request.TermAgreementRequest;
import com.pozit.pozitserver.term.dto.response.TermAgreementItemResponse;
import com.pozit.pozitserver.term.dto.response.TermAgreementResponse;
import com.pozit.pozitserver.term.repository.TermAgreementRepository;
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
public class TermAgreementService {

    // TODO: 정책 확정 후 "이 termType들은 반드시 agreed=true여야 한다" 검증을 여기에 추가한다.
    private final TermAgreementRepository termAgreementRepository;
    private final TermRepository termRepository;

    @Transactional
    public TermAgreementResponse agree(User user, TermAgreementRequest request) {
        for (TermAgreementItemRequest item : request.agreements()) {
            TermAgreement termAgreement = termAgreementRepository.findByUserAndTermType(user, item.termType())
                    .orElseGet(() -> TermAgreement.builder().user(user).termType(item.termType()).build());

            String agreedVersion = Boolean.TRUE.equals(item.agreed())
                    ? getLatestVersionOrNull(item.termType())
                    : null;
            termAgreement.update(Boolean.TRUE.equals(item.agreed()), agreedVersion);
            termAgreementRepository.save(termAgreement);
        }

        return toResponse(termAgreementRepository.findByUser(user));
    }

    public TermAgreementResponse getAgreement(User user) {
        return toResponse(termAgreementRepository.findByUser(user));
    }

    private String getLatestVersionOrNull(String termTypeName) {
        TermType type;
        try {
            type = TermType.valueOf(termTypeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return termRepository.findTopByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(type, LocalDate.now())
                .map(Term::getVersion)
                .orElse(null);
    }

    private TermAgreementResponse toResponse(List<TermAgreement> termAgreements) {
        List<TermAgreementItemResponse> items = termAgreements.stream()
                .map(termAgreement -> new TermAgreementItemResponse(
                        termAgreement.getTermType(),
                        termAgreement.getAgreed(),
                        termAgreement.getAgreedVersion(),
                        termAgreement.getAgreedAt()
                ))
                .toList();
        return new TermAgreementResponse(items);
    }
}
