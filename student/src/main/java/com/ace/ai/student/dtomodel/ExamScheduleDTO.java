package com.ace.ai.student.dtomodel;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ExamScheduleDTO {
    private int id;
    private String examName;
    private String startDate;
    private String endDate;
    private String status;
}
