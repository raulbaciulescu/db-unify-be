package com.raulb.db_unify_be.repository;

import com.raulb.db_unify_be.entity.ScheduledJobResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduledJobResultRepository extends JpaRepository<ScheduledJobResult, Long> {
    List<ScheduledJobResult> findByJobId(Long jobId);
}