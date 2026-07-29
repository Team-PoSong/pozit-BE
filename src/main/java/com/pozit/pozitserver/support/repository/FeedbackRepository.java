package com.pozit.pozitserver.support.repository;

import com.pozit.pozitserver.support.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
