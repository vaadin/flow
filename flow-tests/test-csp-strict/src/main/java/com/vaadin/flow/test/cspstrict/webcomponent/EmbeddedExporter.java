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
package com.vaadin.flow.test.cspstrict.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Exports {@link EmbeddedComponent} as the {@code csp-embedded} web component,
 * for a page that Vaadin does not serve, see {@link EmbeddedHostPage}. The
 * {@code name} property goes from the host page to the server, and the
 * {@code counted} event goes from the server to the host page.
 */
public class EmbeddedExporter extends WebComponentExporter<EmbeddedComponent> {

    /**
     * Tag name of the exported web component.
     */
    public static final String TAG = "csp-embedded";

    public EmbeddedExporter() {
        super(TAG);
        addProperty("name", "").onChange(EmbeddedComponent::setName);
    }

    @Override
    protected void configureInstance(
            WebComponent<EmbeddedComponent> webComponent,
            EmbeddedComponent component) {
        component.setClickHandler(count -> webComponent.fireEvent("counted",
                JacksonUtils.createObjectNode().put("count", count)));
    }
}
