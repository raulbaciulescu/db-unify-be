package com.raulb.db_unify_be.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ScheduledJobResult {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private ScheduledJob job;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private boolean success;
    private String error;
    private String resultPath; // ex: "results/job-12-2025-05-24T20:00.csv"
}