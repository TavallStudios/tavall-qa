package org.tavall.qa.web.pages.venture;

import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.handler.ITaskBoardVentureQueryHandler;
import org.tavall.qa.mapping.TaskBoardVentureMapper;

import java.util.Objects;

/** JSON response body for GET /api/ventures. The host supplies the request-time QA subject. */
public final class QaVenturesJsonRenderer {
    private QaVenturesJsonRenderer() {
    }

    public static String render(ITaskBoardVentureQueryHandler ventures, QaSubjectContext subject) {
        Objects.requireNonNull(ventures, "ventures");
        return TaskBoardVentureMapper.writeDirectory(ventures.findVentures(subject));
    }
}
