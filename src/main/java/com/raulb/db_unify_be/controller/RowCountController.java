package com.raulb.db_unify_be.controller;

import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.repository.ConnectionRepository;
import com.raulb.db_unify_be.service.RowCountEstimator;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/rowcount")
public class RowCountController {

    private final RowCountEstimator rowCountEstimator;
    private final ConnectionRepository connectionRepository;

    public RowCountController(RowCountEstimator rowCountEstimator, ConnectionRepository connectionRepository) {
        this.rowCountEstimator = rowCountEstimator;
        this.connectionRepository = connectionRepository;
    }

    @GetMapping("/{connectionId}/{tableName}")
    public Optional<Long> estimateRowCount(
            @PathVariable Long connectionId,
            @PathVariable String tableName
    ) {
        Connection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        return rowCountEstimator.estimateRowCount(conn, tableName);
                //.map(count -> Map.of("estimatedRows", count));
                //.orElse(Map.of("estimatedRows", null, "message", "Estimation failed or not available"));
                //.orElse(Map.of("estimatedRows", 2));
    }
}
