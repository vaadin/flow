/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
