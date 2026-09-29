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
package com.vaadin.flow.spring.test.nativeimage;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

/**
 * Uses what the application ships as resources: translations under vaadin-i18n,
 * vaadin-featureflags.properties, and a JavaScript module that the browser
 * loads when the server lists it as a dependency of the view.
 */
@Route("resources")
@JsModule("./native-module.js")
public class ResourcesView extends Div {

    public static final String TRANSLATION_ID = "translation";
    public static final String FEATURE_ID = "feature";

    public ResourcesView() {
        Span translation = new Span(getTranslation("greeting"));
        translation.setId(TRANSLATION_ID);
        Span feature = new Span(String.valueOf(
                FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(FeatureFlags.ACCESSIBLE_DISABLED_BUTTONS)));
        feature.setId(FEATURE_ID);
        add(translation, feature);
    }
}
