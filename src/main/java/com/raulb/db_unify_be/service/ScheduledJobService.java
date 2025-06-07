package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.dtos.QueryResult;
import com.raulb.db_unify_be.entity.ScheduledJob;
import com.raulb.db_unify_be.entity.ScheduledJobResult;
import com.raulb.db_unify_be.repository.ScheduledJobRepository;
import com.raulb.db_unify_be.repository.ScheduledJobResultRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledJobService {
    private final ScheduledJobRepository jobRepo;
    private final ScheduledJobResultRepository resultRepo;
    private final QueryService queryService;
    private final TaskScheduler taskScheduler;

    private final Map<Long, CronTrigger> activeJobs = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadJobs() {
        jobRepo.findAll().forEach(this::scheduleJob);
    }

    public void scheduleJob(ScheduledJob job) {
        Runnable task = () -> {
            ScheduledJobResult result = new ScheduledJobResult();
            result.setJob(job);
            result.setStartedAt(LocalDateTime.now());

            try {
                QueryResult queryResult = queryService.execute(job.getQuery(), 0);
                String path = saveToCsv(queryResult);
                result.setResultPath(path);
                result.setSuccess(true);
            } catch (Exception e) {
                result.setError(e.getMessage());
                result.setSuccess(false);
                e.printStackTrace();
            }

            result.setEndedAt(LocalDateTime.now());
            resultRepo.save(result);
        };

        // use Spring’s TaskScheduler (configure in config)
        CronTrigger trigger = new CronTrigger(job.getCron());
        activeJobs.put(job.getId(), trigger);

        taskScheduler.schedule(task, trigger);
    }

    private String saveToCsv(QueryResult result) {
        String directory = "results";
        String fileName = "result-" + System.currentTimeMillis() + ".csv";
        Path resultsDir = Paths.get(directory);
        Path filePath = resultsDir.resolve(fileName);

        try {
            // Creează folderul dacă nu există
            if (!Files.exists(resultsDir)) {
                Files.createDirectories(resultsDir);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                List<Map<String, Object>> rows = result.results();

                if (!rows.isEmpty()) {
                    // Header
                    writer.write(String.join(",", rows.get(0).keySet()));
                    writer.newLine();

                    // Data
                    for (Map<String, Object> row : rows) {
                        String line = row.values().stream()
                                .map(val -> "\"" + String.valueOf(val).replace("\"", "\"\"") + "\"")
                                .collect(Collectors.joining(","));
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }

            System.out.println("✅ CSV saved to: " + filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {
            System.err.println("❌ Failed to save CSV to " + filePath);
            e.printStackTrace();
            throw new RuntimeException("Could not save CSV: " + e.getMessage(), e);
        }
    }

    public void deleteJob(Long jobId) {
        ScheduledJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        // Optional: șterge rezultatele asociate
        List<ScheduledJobResult> results = resultRepo.findByJobId(jobId);
        for (ScheduledJobResult result : results) {
            // Ștergere fișier rezultat dacă există
            if (result.getResultPath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(result.getResultPath()));
                } catch (IOException e) {
                    System.err.println("❌ Could not delete file: " + result.getResultPath());
                    e.printStackTrace();
                }
            }
        }
        resultRepo.deleteAll(results);

        // Remove from activeJobs (dacă folosești mapă internă)
        activeJobs.remove(jobId);

        // Șterge jobul
        jobRepo.deleteById(jobId);
    }

    public List<ScheduledJob> findAllJobs() {
        return jobRepo.findAll();
    }

    public List<ScheduledJobResult> getResults(Long jobId) {
        return resultRepo.findByJobId(jobId);
    }

    public ScheduledJobResult getResultById(Long resultId) {
        return resultRepo.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Result not found with ID: " + resultId));
    }

    public ScheduledJob createJob(ScheduledJob job) {
        job.setCreatedAt(LocalDateTime.now());
        ScheduledJob saved = jobRepo.save(job);
        scheduleJob(saved);
        return saved;
    }
}