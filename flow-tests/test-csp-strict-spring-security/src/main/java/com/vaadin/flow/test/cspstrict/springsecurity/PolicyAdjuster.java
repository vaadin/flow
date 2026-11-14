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
package com.vaadin.flow.test.cspstrict.springsecurity;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.ContentSecurityPolicy;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Adds a directive to the policy that Vaadin sends with a bootstrap page. This
 * is how an application adjusts the policy in the {@code strict} mode, also for
 * what it would otherwise configure in Spring Security.
 */
@Component
public class PolicyAdjuster implements VaadinServiceInitListener {

    public static final String DIRECTIVE = "frame-ancestors";

    public static final String SOURCE = "'none'";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> {
            ContentSecurityPolicy policy = response.getContentSecurityPolicy();
            if (policy != null) {
                policy.setDirective(DIRECTIVE, SOURCE);
            }
        });
    }
}
