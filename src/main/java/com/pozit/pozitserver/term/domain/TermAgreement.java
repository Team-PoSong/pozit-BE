package com.pozit.pozitserver.term.domain;

import com.pozit.pozitserver.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "term_agreements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_term_agreement_user_type",
                columnNames = {"user_id", "term_type"}
        )
)
public class TermAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "term_type", nullable = false, length = 30)
    private String termType;

    @Column(nullable = false)
    private Boolean agreed;

    @Column(name = "agreed_version")
    private String agreedVersion;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    private TermAgreement(User user, String termType) {
        this.user = user;
        this.termType = termType;
    }

    public void update(boolean agreed, String agreedVersion) {
        this.agreed = agreed;
        this.agreedVersion = agreed ? agreedVersion : null;
        this.agreedAt = agreed ? LocalDateTime.now() : null;
    }
}
