/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.pushstartup;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Holds service initialization open so that a push connection has time to
 * arrive while the service is not able to process requests yet.
 * <p>
 * Service init listeners run after the push endpoint has been wired to the
 * service, but before the service reports itself as initialized, so sleeping
 * here widens exactly the window a slow listener opens in a real application.
 */
@Component
public class SlowServiceInitListener implements VaadinServiceInitListener {

    /**
     * How long service initialization is held open. Long enough for the test to
     * notice that initialization has started and open a push connection while
     * it is still running.
     */
    public static final long INIT_DELAY_MS = 8000;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        try {
            Thread.sleep(INIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
