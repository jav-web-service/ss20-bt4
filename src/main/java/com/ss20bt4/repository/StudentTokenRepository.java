package com.ss20bt4.repository;

import com.ss20bt4.entity.StudentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentTokenRepository extends JpaRepository<StudentToken, Long> {
    Optional<StudentToken> findByTokenString(String tokenString);

    @Query("SELECT t FROM StudentToken t WHERE t.student.id = :studentId AND t.isRevoked = false AND t.isExpired = false")
    List<StudentToken> findAllValidTokensByStudentId(@Param("studentId") Long studentId);
}
