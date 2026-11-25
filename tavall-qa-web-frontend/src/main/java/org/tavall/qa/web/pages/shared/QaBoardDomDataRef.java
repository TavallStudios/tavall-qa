package org.tavall.qa.web.pages.shared;

import org.tavall.web.api.html.DOMDataRef;

/** Stable DOM hooks for the existing progressive task-list behavior. */
public enum QaBoardDomDataRef implements DOMDataRef {
    TASK_GROUP("data-tavall-qa-task-group"),
    TASK_ITEM("data-tavall-qa-task-item"),
    TASK_EXPANDER("data-tavall-qa-task-expander");

    private final String value;

    QaBoardDomDataRef(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
