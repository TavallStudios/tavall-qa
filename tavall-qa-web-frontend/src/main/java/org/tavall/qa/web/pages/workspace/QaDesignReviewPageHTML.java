package org.tavall.qa.web.pages.workspace;

import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardDesignReview;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.qa.web.generated.QaCSS;
import org.tavall.qa.web.pages.shared.QaProduct;
import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.web.api.html.HTMLAttribute;
import org.tavall.web.api.html.HTMLDocument;
import org.tavall.web.api.html.HTMLElement;
import org.tavall.web.frontend.design.Forge;
import org.tavall.web.frontend.html.HTML;

import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.TreeMap;

final class QaDesignReviewPageHTML {
    private QaDesignReviewPageHTML() { }

    static HTMLDocument build(TaskBoardTaskView view, QaWorkspacePageContext context, boolean canSelect,
                              List<TaskBoardTaskView> openReviews) {
        TaskBoardDesignReview review = Objects.requireNonNull(view.designReview(), "designReview");
        HTMLElement shell = HTML.main().classes(QaCSS.QA_APP, QaCSS.PUBLIC_BOARD_SHELL, QaCSS.QA_DESIGN_REVIEW)
                .data(QaDesignReviewDomDataRef.REVIEW, view.task().taskId().toString());
        shell.child(HTML.a().attribute(HTMLAttribute.HREF, QaRoutes.task(view.task().projectKey(), view.task().taskId()))
                .text("Back to task"));
        shell.child(HTML.h1().text(view.task().title()));
        shell.child(HTML.p().text("Surface: " + review.surface()));
        shell.child(HTML.p().text("Compare the live candidates, choose a viewport and try the display options."));
        HTMLElement open = HTML.nav().attribute(HTMLAttribute.ARIA_LABEL, "Open design reviews");
        open.child(HTML.h2().text("Open design reviews"));
        HTMLElement openList = HTML.ul();
        for (TaskBoardTaskView item : openReviews) {
            if (item.designReview() == null || item.designReview().decided()) continue;
            openList.child(HTML.li().child(HTML.a()
                    .attribute(HTMLAttribute.HREF, QaRoutes.designReview(item.task().projectKey(), item.task().taskId()))
                    .text(item.designReview().surface() + " · " + item.task().title())));
        }
        open.child(openList);
        shell.child(open);
        if (review.decided()) {
            shell.child(HTML.p().attribute(HTMLAttribute.ROLE, "status").text("Selected: " + review.winnerKey()
                    + " · " + review.decidedAt() + " · Account " + review.decidedByAccountId()));
            if (!review.note().isEmpty()) shell.child(HTML.p().text(review.note()));
        } else if (!canSelect) {
            shell.child(HTML.p().text("An owner or assigned reviewer can select the winner."));
        }
        HTMLElement controls = HTML.section().classes(QaCSS.QA_DESIGN_CONTROLS)
                .attribute(HTMLAttribute.ARIA_LABEL, "Review controls");
        for (TaskBoardDesignCandidate candidate : review.candidates()) {
            controls.child(HTML.button().classes(QaCSS.GHOST_BUTTON).attribute(HTMLAttribute.TYPE, "button")
                    .data(QaDesignReviewDomDataRef.CANDIDATE, candidate.key())
                    .attribute(HTMLAttribute.ARIA_PRESSED, "false").text(candidate.key() + " · " + candidate.name()));
        }
        controls.child(select("View", "view", List.of("single", "compare")));
        controls.child(select("Viewport", "width", List.of("1440", "768", "390")));
        Map<String, java.util.Set<String>> options = new TreeMap<>();
        review.candidates().forEach(candidate -> candidate.optionParameters().forEach((key, values) ->
                options.computeIfAbsent(key, ignored -> new java.util.TreeSet<>()).addAll(values)));
        options.forEach((key, values) -> controls.child(select(key, key, List.copyOf(values))));
        shell.child(controls);

        HTMLElement status = HTML.section().classes(QaCSS.PUBLIC_SECTION_CARD);
        status.child(HTML.h2().text("Review status"));
        if (review.decided()) {
            status.child(HTML.p().text("Decided by human: " + review.winnerKey()));
            status.child(HTML.p().text("Deployable: Yes"));
        } else if (review.aiReviewed()) {
            status.child(HTML.p().text("AI reviewed (" + review.aiCandidateKey() + ", " + review.aiReviewerModel()
                    + ") · Pending human · Deployable"));
        } else {
            status.child(HTML.p().text("Awaiting review"));
            status.child(HTML.p().text("Deployable: No"));
        }
        if (review.aiReviewed()) {
            status.child(HTML.p().text("AI candidate: " + review.aiCandidateKey()));
            status.child(HTML.p().text("AI rationale: " + review.aiRationale()));
            status.child(HTML.p().text("AI reviewer agent: " + review.aiReviewerAgentIdentity()));
            if (review.aiReviewedAt() != null) {
                status.child(HTML.p().text("AI reviewed at: " + review.aiReviewedAt()));
            }
            if (!review.aiEvidenceRefs().isEmpty()) {
                status.child(HTML.p().text("Evidence refs: " + String.join(", ", review.aiEvidenceRefs())));
            }
            if (!review.aiArtifactRefs().isEmpty()) {
                status.child(HTML.p().text("Artifact refs: " + String.join(", ", review.aiArtifactRefs())));
            }
            if (review.aiScreenshotArtifactRef() != null && !review.aiScreenshotArtifactRef().isBlank()) {
                status.child(HTML.p().text("Screenshot artifact: " + review.aiScreenshotArtifactRef()));
            }
        }
        shell.child(status);

        HTMLElement frames = HTML.div().classes(QaCSS.QA_DESIGN_FRAMES);
        for (TaskBoardDesignCandidate candidate : review.candidates()) {
            HTMLElement card = HTML.section().classes(QaCSS.QA_DESIGN_CARD)
                    .data(QaDesignReviewDomDataRef.CARD, candidate.key());
            card.child(HTML.h2().text(candidate.key() + " · " + candidate.name()));
            card.child(HTML.p().text(candidate.rationale()));
            String exposedOptions = candidate.optionParameters().isEmpty() ? "No adjustable preview options"
                    : "Options: " + candidate.optionParameters().entrySet().stream()
                            .map(entry -> entry.getKey() + " (" + String.join(", ", entry.getValue()) + ")")
                            .sorted().collect(java.util.stream.Collectors.joining(" · "));
            card.child(HTML.p().text(exposedOptions));
            card.child(HTML.a().attribute(HTMLAttribute.HREF, candidate.previewUrl()).text("Open live preview"));
            card.child(HTML.div().classes(QaCSS.QA_DESIGN_VIEWPORT).data(QaDesignReviewDomDataRef.VIEWPORT, candidate.key())
                    .child(HTML.iframe().attribute(HTMLAttribute.SRC, candidate.previewUrl())
                            .attribute(HTMLAttribute.TITLE, "Live preview: " + candidate.name())
                            .attribute(HTMLAttribute.SANDBOX, "allow-scripts allow-same-origin")
                            .attribute(HTMLAttribute.REFERRERPOLICY, "no-referrer")
                            .attribute(HTMLAttribute.WIDTH, "1440").attribute(HTMLAttribute.HEIGHT, "900")
                            .data(QaDesignReviewDomDataRef.FRAME, candidate.key())));
            if (!review.decided() && canSelect) {
                HTMLElement form = HTML.form().attribute(HTMLAttribute.METHOD, "post")
                        .attribute(HTMLAttribute.ACTION, QaRoutes.designReviewDecision(view.task().projectKey(), view.task().taskId()));
                form.child(hidden(context.csrfParameterName(), context.csrfToken()));
                context.organizationId().ifPresent(id -> form.child(hidden("organizationId", id.toString())));
                form.child(hidden("candidateKey", candidate.key()));
                form.child(hidden("expectedRevision", Integer.toString(review.revision())));
                form.child(HTML.label().text("Decision note (optional)")
                        .child(HTML.textarea().attribute(HTMLAttribute.NAME, "note").attribute(HTMLAttribute.MAXLENGTH, "4000")));
                form.child(HTML.button().classes(QaCSS.GHOST_BUTTON).attribute(HTMLAttribute.TYPE, "submit")
                        .text("Select " + candidate.key() + " as winner"));
                card.child(form);
            }
            frames.child(card);
        }
        shell.child(frames);
        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }

    private static HTMLElement select(String label, String key, List<String> values) {
        HTMLElement select = HTML.select().data(QaDesignReviewDomDataRef.CONTROL, key)
                .attribute(HTMLAttribute.ARIA_LABEL, label);
        for (String value : values) select.child(HTML.option().attribute(HTMLAttribute.VALUE, value).text(value));
        return HTML.label().text(label + " ").child(select);
    }

    private static HTMLElement hidden(String name, String value) {
        return HTML.input().attribute(HTMLAttribute.TYPE, "hidden").attribute(HTMLAttribute.NAME, name)
                .attribute(HTMLAttribute.VALUE, value);
    }
}
