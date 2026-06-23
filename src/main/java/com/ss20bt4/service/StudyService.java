package com.ss20bt4.service;

import com.ss20bt4.client.CertificateClient;
import com.ss20bt4.dto.CertificateRequest;
import com.ss20bt4.dto.CertificateResponse;
import com.ss20bt4.dto.StudyProgressResponse;
import com.ss20bt4.entity.Course;
import com.ss20bt4.entity.Enrollment;
import com.ss20bt4.entity.Student;
import com.ss20bt4.exception.CourseNotCompletedException;
import com.ss20bt4.repository.CourseRepository;
import com.ss20bt4.repository.EnrollmentRepository;
import com.ss20bt4.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CertificateClient certificateClient;

    public StudyProgressResponse getMyProgress() {
        // Lấy student_id từ Security Context
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long studentId = userDetails.getStudent().getId();

        // Truy xuất tất cả các Enrollment của sinh viên này
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        // Lọc ra những khóa học đã hoàn thành 100%
        // Chuyển đổi danh sách thực thể thành danh sách tên khóa học
        List<String> graduatedCourses = enrollments.stream()
                .filter(e -> e.getLessonsCompleted().equals(e.getCourse().getTotalLessons()))
                .map(e -> e.getCourse().getTitle())
                .collect(Collectors.toList());

        // Tính tổng số bài học (lessons) sinh viên đã hoàn thành trên tất cả các khóa
        Integer totalCompletedLessons = enrollments.stream()
                .mapToInt(Enrollment::getLessonsCompleted)
                .sum();

        return StudyProgressResponse.builder()
                .totalCompletedLessons(totalCompletedLessons)
                .graduatedCourses(graduatedCourses)
                .build();
    }

    public CertificateResponse claimCertificate(Long courseId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Student student = userDetails.getStudent();

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        // Sử dụng Stream API để tìm khóa học và kiểm tra hoàn thành
        Enrollment courseEnrollment = enrollments.stream()
                .filter(e -> e.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrollment not found for the given course"));

        Course course = courseEnrollment.getCourse();

        if (!courseEnrollment.getLessonsCompleted().equals(course.getTotalLessons())) {
            throw new CourseNotCompletedException("Student has not completed the course: " + course.getTitle());
        }

        CertificateRequest request = CertificateRequest.builder()
                .studentName(student.getFullName())
                .courseTitle(course.getTitle())
                .build();

        // Gọi OpenFeign
        return certificateClient.claimCertificate(request);
    }
}
