package com.ss20bt4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyProgressResponse {
    private Integer totalCompletedLessons;
    private List<String> graduatedCourses;
}
