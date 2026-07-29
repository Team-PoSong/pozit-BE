package com.pozit.pozitserver.term.repository;

import com.pozit.pozitserver.term.domain.Term;
import com.pozit.pozitserver.term.domain.TermType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findTopByTypeOrderByCreatedAtDesc(TermType type);
}
