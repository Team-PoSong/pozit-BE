package com.pozit.pozitserver.term.repository;

import com.pozit.pozitserver.term.domain.TermAgreement;
import com.pozit.pozitserver.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermAgreementRepository extends JpaRepository<TermAgreement, Long> {

    List<TermAgreement> findByUser(User user);

    Optional<TermAgreement> findByUserAndTermType(User user, String termType);
}
