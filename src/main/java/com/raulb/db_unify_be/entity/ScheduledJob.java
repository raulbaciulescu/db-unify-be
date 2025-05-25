package com.raulb.db_unify_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ScheduledJob {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String cron;
    @Column(length = 5000)
    private String query;
    private LocalDateTime createdAt;
}
