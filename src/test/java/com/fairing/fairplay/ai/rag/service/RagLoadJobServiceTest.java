package com.fairing.fairplay.ai.rag.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagLoadJobServiceTest {

    @Test
    void startsComprehensiveLoadInExecutorAndStoresResult() {
        ComprehensiveRagDataLoader dataLoader = mock(ComprehensiveRagDataLoader.class);
        ComprehensiveRagDataLoader.ComprehensiveLoadResult loadResult = successfulResult();
        when(dataLoader.loadAllPublicData()).thenReturn(loadResult);
        RagLoadJobService service = new RagLoadJobService(dataLoader, Runnable::run);

        RagLoadJobService.JobSnapshot started = service.startComprehensiveLoad();
        RagLoadJobService.JobSnapshot latest = service.getLatestJob().orElseThrow();

        assertThat(started.getStatus()).isEqualTo(RagLoadJobService.JobStatus.RUNNING);
        assertThat(latest.getStatus()).isEqualTo(RagLoadJobService.JobStatus.SUCCEEDED);
        assertThat(latest.getSummary()).contains("Event");
        assertThat(latest.getTotalFailCount()).isZero();
        verify(dataLoader).loadAllPublicData();
    }

    @Test
    void returnsCurrentRunningJobInsteadOfStartingDuplicateLoad() {
        ComprehensiveRagDataLoader dataLoader = mock(ComprehensiveRagDataLoader.class);
        AtomicReference<Runnable> pendingTask = new AtomicReference<>();
        AtomicInteger executeCount = new AtomicInteger();
        Executor capturingExecutor = task -> {
            executeCount.incrementAndGet();
            pendingTask.set(task);
        };
        RagLoadJobService service = new RagLoadJobService(dataLoader, capturingExecutor);

        RagLoadJobService.JobSnapshot first = service.startComprehensiveLoad();
        RagLoadJobService.JobSnapshot second = service.startComprehensiveLoad();

        assertThat(first.getJobId()).isEqualTo(second.getJobId());
        assertThat(second.isAlreadyRunning()).isTrue();
        assertThat(executeCount).hasValue(1);
        assertThat(pendingTask.get()).isNotNull();
    }

    private ComprehensiveRagDataLoader.ComprehensiveLoadResult successfulResult() {
        ComprehensiveRagDataLoader.ComprehensiveLoadResult result =
            new ComprehensiveRagDataLoader.ComprehensiveLoadResult();
        result.eventResult = new ComprehensiveRagDataLoader.LoadResult("Event", 1, 1, 0);
        result.boothResult = new ComprehensiveRagDataLoader.LoadResult("Booth", 1, 1, 0);
        result.boothExperienceResult = new ComprehensiveRagDataLoader.LoadResult("BoothExperience", 1, 1, 0);
        result.reviewResult = new ComprehensiveRagDataLoader.LoadResult("Review", 1, 1, 0);
        result.categoryResult = new ComprehensiveRagDataLoader.LoadResult("Category", 0, 0, 0);
        result.userDataResult = new ComprehensiveRagDataLoader.LoadResult("UserData", 1, 1, 0);
        return result;
    }
}
