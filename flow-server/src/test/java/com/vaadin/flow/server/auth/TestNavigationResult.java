/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.auth;

import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.internal.ErrorStateRenderer;

class TestNavigationResult {

    public BeforeEnterEvent event;
    public Map<String, Object> sessionAttributes;

    public Class<? extends Component> getReroutedTo() {
        if (!event.hasRerouteTarget()
                || event.getRerouteTarget() instanceof ErrorStateRenderer) {
            return null;
        }
        return event.getRerouteTargetType();
    }

    public Class<? extends Component> getForwardedTo() {
        if (!event.hasForwardTarget()) {
            return null;
        }
        return event.getForwardTargetType();
    }

    public String getRerouteErrorMessage() {
        if (!event.hasRerouteTarget()
                || !(event.getRerouteTarget() instanceof ErrorStateRenderer)) {
            return null;
        }

        return event.getErrorParameter().getCustomMessage();
    }

    public Class<? extends Exception> getRerouteError() {
        if (!event.hasRerouteTarget()
                || !(event.getRerouteTarget() instanceof ErrorStateRenderer)) {
            return null;
        }

        return event.getErrorParameter().getException().getClass();
    }

    public String getRerouteURL() {
        if (event.hasUnknownForward()) {
            return event.getUnknownForward();
        } else {
            return null;
        }
    }

    public String getExternalForwardUrl() {
        if (event.hasExternalForwardUrl()) {
            return event.getExternalForwardUrl();
        } else {
            return null;
        }
    }

    public boolean wasTargetViewRendered() {
        return getReroutedTo() == null && getForwardedTo() == null
                && getRerouteError() == null && getRerouteURL() == null
                && getExternalForwardUrl() == null;
    }

}
