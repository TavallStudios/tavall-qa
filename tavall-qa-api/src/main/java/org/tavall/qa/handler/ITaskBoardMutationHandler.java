package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.request.TaskBoardAssignmentCreateRequest;
import org.tavall.qa.request.TaskBoardAssignmentRevokeRequest;
import org.tavall.qa.request.TaskBoardActionCreateRequest;
import org.tavall.qa.request.TaskBoardCommentCreateRequest;
import org.tavall.qa.request.TaskBoardEvidenceReviewRequest;
import org.tavall.qa.request.TaskBoardEvidenceSubmissionRequest;
import org.tavall.qa.request.TaskBoardHumanVerificationRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskAcceptanceRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.time.Instant;
import java.util.UUID;

public interface ITaskBoardMutationHandler {
    TaskBoardProject createProject(QaSubjectContext subject, TaskBoardProjectCreateRequest request);

    TaskBoardTask createTask(QaSubjectContext subject, TaskBoardTaskCreateRequest request);

    TaskBoardTask updateTask(QaSubjectContext subject, TaskBoardTaskUpdateRequest request);

    TaskBoardTask verifyTask(QaSubjectContext subject, TaskBoardHumanVerificationRequest request);

    TaskBoardTask acceptTask(QaSubjectContext subject, TaskBoardTaskAcceptanceRequest request);

    TaskBoardTask rejectTask(
            QaSubjectContext subject,
            UUID taskId,
            org.tavall.qa.domain.auth.QaEnvironmentGrant environment,
            String reason
    );

    TaskBoardAssignment assignTask(QaSubjectContext subject, TaskBoardAssignmentCreateRequest request);

    TaskBoardAssignment revokeAssignment(QaSubjectContext subject, TaskBoardAssignmentRevokeRequest request);

    TaskBoardEvidence submitEvidence(QaSubjectContext subject, TaskBoardEvidenceSubmissionRequest request);

    TaskBoardEvidence reviewEvidence(QaSubjectContext subject, TaskBoardEvidenceReviewRequest request);

    TaskBoardAction addAction(QaSubjectContext subject, TaskBoardActionCreateRequest request);

    TaskBoardComment addComment(QaSubjectContext subject, TaskBoardCommentCreateRequest request);
}
