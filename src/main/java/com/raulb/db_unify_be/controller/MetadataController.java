package com.raulb.db_unify_be.controller;

import com.raulb.db_unify_be.entity.TableInfo;
import com.raulb.db_unify_be.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/metadata")
@CrossOrigin
public class MetadataController {

    @Autowired
    private MetadataService metadataService;

    @GetMapping("/{connectionId}/tables")
    public ResponseEntity<List<TableInfo>> getTables(@PathVariable Long connectionId) {
        List<TableInfo> result = metadataService.getTablesAndColumns(connectionId);
        return ResponseEntity.ok(result);
    }
}
