package org.tavall.qa.web.pages.workspace;

import org.tavall.web.api.html.DOMDataRef;

/** Stable DOM hooks shared by the accepted Focus Workspace and its progressive enhancement. */
public enum QaWorkspaceDomDataRef implements DOMDataRef {
    VARIANT("data-tavall-qa-workspace-variant"),
    STATE_PREVIEW("data-tavall-qa-state-preview"),
    FOCUS("data-tavall-qa-workspace-focus"),
    PANEL("data-tavall-qa-workspace-panel");

    private final String value;

    QaWorkspaceDomDataRef(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
