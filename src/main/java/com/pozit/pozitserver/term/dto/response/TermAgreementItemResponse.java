package com.pozit.pozitserver.term.dto.response;

import java.time.LocalDateTime;

public record TermAgreementItemResponse(
        String termType,
        Boolean agreed,
        String agreedVersion,
        LocalDateTime agreedAt
) {}
