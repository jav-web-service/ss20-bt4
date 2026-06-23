package com.ss20bt4.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_string", nullable = false, unique = true, length = 1000)
    private String tokenString;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked;

    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
}
