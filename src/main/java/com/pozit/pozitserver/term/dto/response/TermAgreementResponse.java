package com.pozit.pozitserver.term.dto.response;

import java.util.List;

public record TermAgreementResponse(
        List<TermAgreementItemResponse> agreements
) {}
