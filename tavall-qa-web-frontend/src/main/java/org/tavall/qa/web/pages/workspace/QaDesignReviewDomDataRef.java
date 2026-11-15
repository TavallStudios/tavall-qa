package org.tavall.qa.web.pages.workspace;

import org.tavall.web.api.html.DOMDataRef;

enum QaDesignReviewDomDataRef implements DOMDataRef {
    REVIEW("data-qa-design-review"), CANDIDATE("data-qa-design-candidate"),
    CARD("data-qa-design-card"), FRAME("data-qa-design-frame"), CONTROL("data-qa-design-control"),
    VIEWPORT("data-qa-design-viewport");
    private final String value;
    QaDesignReviewDomDataRef(String value) { this.value = value; }
    @Override public String value() { return value; }
}
