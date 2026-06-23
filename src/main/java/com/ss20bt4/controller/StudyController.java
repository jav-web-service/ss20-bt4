package com.ss20bt4.controller;

import com.ss20bt4.dto.CertificateResponse;
import com.ss20bt4.dto.StudyProgressResponse;
import com.ss20bt4.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/elearning/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @GetMapping("/my-progress")
    public ResponseEntity<StudyProgressResponse> getMyProgress() {
        return ResponseEntity.ok(studyService.getMyProgress());
    }

    @PostMapping("/{courseId}/claim-certificate")
    public ResponseEntity<CertificateResponse> claimCertificate(@PathVariable Long courseId) {
        return ResponseEntity.ok(studyService.claimCertificate(courseId));
    }
}
