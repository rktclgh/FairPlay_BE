package com.fairing.fairplay.ai.rag.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class RagLoadJobService {

    private final ComprehensiveRagDataLoader comprehensiveRagDataLoader;
    private final Executor ragAdminTaskExecutor;
    private final ConcurrentHashMap<String, JobSnapshot> jobs = new ConcurrentHashMap<>();
    private final AtomicReference<String> latestJobId = new AtomicReference<>();
    private final Object startLock = new Object();

    public RagLoadJobService(
            ComprehensiveRagDataLoader comprehensiveRagDataLoader,
            @Qualifier("ragAdminTaskExecutor") Executor ragAdminTaskExecutor) {
        this.comprehensiveRagDataLoader = comprehensiveRagDataLoader;
        this.ragAdminTaskExecutor = ragAdminTaskExecutor;
    }

    public JobSnapshot startComprehensiveLoad() {
        synchronized (startLock) {
            Optional<JobSnapshot> runningJob = getLatestJob()
                .filter(job -> JobStatus.RUNNING.equals(job.getStatus()));
            if (runningJob.isPresent()) {
                JobSnapshot job = runningJob.get().withAlreadyRunning(true);
                jobs.put(job.getJobId(), job);
                return job;
            }

            JobSnapshot job = JobSnapshot.running(UUID.randomUUID().toString());
            jobs.put(job.getJobId(), job);
            latestJobId.set(job.getJobId());
            ragAdminTaskExecutor.execute(() -> runComprehensiveLoad(job.getJobId()));
            return job;
        }
    }

    public Optional<JobSnapshot> getLatestJob() {
        String jobId = latestJobId.get();
        if (jobId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobs.get(jobId));
    }

    public Optional<JobSnapshot> getJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    private void runComprehensiveLoad(String jobId) {
        try {
            log.info("RAG comprehensive load job started: {}", jobId);
            ComprehensiveRagDataLoader.ComprehensiveLoadResult result =
                comprehensiveRagDataLoader.loadAllPublicData();
            jobs.computeIfPresent(jobId, (id, job) -> job.succeeded(result));
            log.info("RAG comprehensive load job succeeded: {}", jobId);
        } catch (Exception e) {
            jobs.computeIfPresent(jobId, (id, job) -> job.failed(e));
            log.error("RAG comprehensive load job failed: {}", jobId, e);
        }
    }

    public enum JobStatus {
        RUNNING,
        SUCCEEDED,
        FAILED
    }

    @Getter
    public static class JobSnapshot {
        private final String jobId;
        private final JobStatus status;
        private final Instant startedAt;
        private final Instant finishedAt;
        private final boolean alreadyRunning;
        private final String summary;
        private final Integer totalSuccessCount;
        private final Integer totalFailCount;
        private final String errorMessage;

        private JobSnapshot(
                String jobId,
                JobStatus status,
                Instant startedAt,
                Instant finishedAt,
                boolean alreadyRunning,
                String summary,
                Integer totalSuccessCount,
                Integer totalFailCount,
                String errorMessage) {
            this.jobId = jobId;
            this.status = status;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.alreadyRunning = alreadyRunning;
            this.summary = summary;
            this.totalSuccessCount = totalSuccessCount;
            this.totalFailCount = totalFailCount;
            this.errorMessage = errorMessage;
        }

        static JobSnapshot running(String jobId) {
            return new JobSnapshot(jobId, JobStatus.RUNNING, Instant.now(), null, false, null, null, null, null);
        }

        JobSnapshot withAlreadyRunning(boolean alreadyRunning) {
            return new JobSnapshot(
                jobId,
                status,
                startedAt,
                finishedAt,
                alreadyRunning,
                summary,
                totalSuccessCount,
                totalFailCount,
                errorMessage
            );
        }

        JobSnapshot succeeded(ComprehensiveRagDataLoader.ComprehensiveLoadResult result) {
            return new JobSnapshot(
                jobId,
                JobStatus.SUCCEEDED,
                startedAt,
                Instant.now(),
                false,
                result.getSummary(),
                result.getTotalSuccessCount(),
                result.getTotalFailCount(),
                null
            );
        }

        JobSnapshot failed(Exception e) {
            return new JobSnapshot(
                jobId,
                JobStatus.FAILED,
                startedAt,
                Instant.now(),
                false,
                summary,
                totalSuccessCount,
                totalFailCount,
                e.getMessage()
            );
        }
    }
}
