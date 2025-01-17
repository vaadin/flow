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

package com.vaadin.flow.router.internal;

import com.vaadin.flow.component.Component;

/**
 * Client route target stores the target template.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
 */
public class ClientTarget extends RouteTarget {
    private final String template;

    /**
     * Create a new Client route target holder with the given route template.
     *
     * @param template
     *            route template
     */
    public ClientTarget(String template) {
        super(Component.class);
        this.template = template;
    }

    /**
     * Get the route template.
     *
     * @return route template
     */
    String getTemplate() {
        return template;
    }
}
