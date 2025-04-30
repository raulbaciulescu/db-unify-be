package com.raulb.db_unify_be.controller;

import com.raulb.db_unify_be.service.SelectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/select")
public class SelectController {

    private final SelectService selectService;

    public SelectController(SelectService selectService) {
        this.selectService = selectService;
    }

    @GetMapping("/{connectionId}/{tableName}")
    public List<Map<String, Object>> selectAll(
            @PathVariable Long connectionId,
            @PathVariable String tableName
    ) {
        return selectService.selectAllFromTable(connectionId, tableName);
    }
}
