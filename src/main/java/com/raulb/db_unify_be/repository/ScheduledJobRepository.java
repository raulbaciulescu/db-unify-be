package com.raulb.db_unify_be.repository;

import com.raulb.db_unify_be.entity.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {}
