package org.tavall.qa.cli;

import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliDesignAiReviewRequest;
import org.tavall.qa.cli.client.QaCliDesignAiReviewResult;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliDesignRegisterRequest;
import org.tavall.qa.cli.client.QaCliDesignRegisterResult;
import org.tavall.qa.cli.client.QaCliDesignReviewSnapshot;
import org.tavall.qa.cli.client.QaCliTaskUpsertRequest;
import org.tavall.qa.cli.client.QaCliTaskUpsertResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Deterministic in-memory QA execution client for command handler tests. */
final class StubQaExecutionApiClient implements IQaExecutionApiClient {
    QaCliDesignAiReviewRequest lastAiReviewRequest;
    QaCliDesignAiReviewResult nextAiReviewResult;

    @Override
    public QaCliDesignAiReviewResult recordDesignAiReview(QaCliDesignAiReviewRequest request) {
        lastAiReviewRequest = request;
        if (nextFailure != null) throw nextFailure;
        return nextAiReviewResult;
    }

    QaCliTaskUpsertRequest lastUpsertRequest;
    QaCliTaskUpsertResult nextUpsertResult;
    UUID lastDesignReviewTaskId;
    Path lastDesignReviewCredentialFile;
    Optional<QaCliDesignReviewSnapshot> nextDesignReview = Optional.empty();
    QaCliDesignRegisterRequest lastRegisterRequest;
    QaCliDesignRegisterResult nextRegisterResult;
    QaCliApiClientException nextFailure;

    @Override
    public QaCliTaskUpsertResult upsertTask(QaCliTaskUpsertRequest request) {
        lastUpsertRequest = request;
        throwIfFailing();
        return nextUpsertResult;
    }

    @Override
    public Optional<QaCliDesignReviewSnapshot> getDesignReview(UUID taskId, Path credentialFile) {
        lastDesignReviewTaskId = taskId;
        lastDesignReviewCredentialFile = credentialFile;
        throwIfFailing();
        return nextDesignReview;
    }

    @Override
    public QaCliDesignRegisterResult registerDesignCandidates(QaCliDesignRegisterRequest request) {
        lastRegisterRequest = request;
        throwIfFailing();
        return nextRegisterResult;
    }

    private void throwIfFailing() {
        if (nextFailure != null) {
            throw nextFailure;
        }
    }

    static QaCliTaskUpsertResult upsertResult(UUID taskId, String externalKey) {
        return new QaCliTaskUpsertResult(taskId, org.tavall.qa.domain.TaskBoardTaskStatus.ACTIVE,
                true, false, externalKey);
    }

    static QaCliDesignRegisterResult registerResult(UUID taskId, int revision, List<String> keys) {
        return new QaCliDesignRegisterResult(taskId, revision, keys, null, false);
    }
}
