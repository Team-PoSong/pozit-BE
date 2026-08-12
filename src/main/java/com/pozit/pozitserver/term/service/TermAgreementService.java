package com.pozit.pozitserver.term.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
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
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermAgreementService {

    // 아직 기획 확정 전이라 termType은 자유 문자열이지만, 지금 화면 기준으로 이 4개는 필수로 둔다.
    private static final List<String> REQUIRED_TERM_TYPES = List.of("SERVICE", "PRIVACY", "LOCATION", "AGE_OVER_14");

    private final TermAgreementRepository termAgreementRepository;
    private final TermRepository termRepository;
    private final UserRepository userRepository;

    @Transactional
    public TermAgreementResponse agree(User user, TermAgreementRequest request) {
        Map<String, Boolean> agreedByType = request.agreements().stream()
                .collect(Collectors.toMap(TermAgreementItemRequest::termType, TermAgreementItemRequest::agreed));

        boolean allRequiredAgreed = REQUIRED_TERM_TYPES.stream()
                .allMatch(type -> Boolean.TRUE.equals(agreedByType.get(type)));
        if (!allRequiredAgreed) {
            throw new BusinessException(ErrorCode.REQUIRED_TERM_NOT_AGREED);
        }

        userRepository.findByIdForUpdate(user.getId());

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
