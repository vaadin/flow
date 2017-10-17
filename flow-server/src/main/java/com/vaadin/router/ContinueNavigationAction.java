/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.router;

import java.io.Serializable;

import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.NavigationEvent;

/**
 * The action to resume a postponed {@link BeforeNavigationEvent}.
 * @author Vaadin Ltd.
 */
public class ContinueNavigationAction implements Serializable {

    private NavigationHandler handler = null;
    private NavigationEvent event = null;

    void setReferences(NavigationHandler handler, NavigationEvent event) {
        this.handler = handler;
        this.event = event;
    }

    /**
     * Resumes the page transition associated with the postponed event.
     */
    public void proceed() {
        if (handler != null && event != null) {
            handler.handle(event);
            setReferences(null, null);
        }
    }
}
