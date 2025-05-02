package com.raulb.db_unify_be.repository;

import com.raulb.db_unify_be.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Optional<Connection> findByName(String name);
}