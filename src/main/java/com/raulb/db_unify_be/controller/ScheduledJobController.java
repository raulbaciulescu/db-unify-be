package com.raulb.db_unify_be.controller;

import com.raulb.db_unify_be.entity.ScheduledJob;
import com.raulb.db_unify_be.entity.ScheduledJobResult;
import com.raulb.db_unify_be.service.ScheduledJobService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/scheduled-jobs")
@RequiredArgsConstructor
@CrossOrigin
public class ScheduledJobController {
    private final ScheduledJobService service;

    @PostMapping
    public ResponseEntity<ScheduledJob> create(@RequestBody ScheduledJob job) {
        return ResponseEntity.ok(service.createJob(job));
    }

    @GetMapping
    public List<ScheduledJob> list() {
        return service.findAllJobs();
    }

    @GetMapping("/{id}/results")
    public List<ScheduledJobResult> getResults(@PathVariable Long id) {
        return service.getResults(id);
    }

    @GetMapping("/results/{resultId}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long resultId) throws IOException {
        ScheduledJobResult result = service.getResultById(resultId);
        Path path = Paths.get(result.getResultPath());

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + path.getFileName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.deleteJob(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}