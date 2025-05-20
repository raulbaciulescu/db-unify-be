package com.raulb.db_unify_be.controller;

import com.raulb.db_unify_be.service.DataFetcher;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/select")
public class SelectController {

    private final DataFetcher dataFetcher;

    public SelectController(DataFetcher dataFetcher) {
        this.dataFetcher = dataFetcher;
    }

    @GetMapping("/{connectionId}/{tableName}")
    public List<Map<String, Object>> selectAll(
            @PathVariable Long connectionId,
            @PathVariable String tableName
    ) {
        return dataFetcher.selectAllFromTable(connectionId, tableName);
    }
}
