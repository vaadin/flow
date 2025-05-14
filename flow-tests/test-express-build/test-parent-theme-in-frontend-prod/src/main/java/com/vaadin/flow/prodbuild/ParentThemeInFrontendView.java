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

package com.vaadin.flow.prodbuild;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.prodbuild.ParentThemeInFrontendView")
public class ParentThemeInFrontendView extends Div {

    public ParentThemeInFrontendView() {
        Div staticResource = new Div();
        staticResource.setHeight("360px");
        staticResource.setWidth("360px");
        staticResource.addClassName("vaadin-logo");
        staticResource.setId("vaadin-logo");
        add(staticResource);

        Div themeResource = new Div();
        themeResource.setHeight("360px");
        themeResource.setWidth("360px");
        themeResource.addClassName("hilla-logo");
        themeResource.setId("hilla-logo");
        add(themeResource);

        add(new Paragraph("Red color text from parent 'reusable-theme'"));
        add(new Span("Green color text from parent 'other-theme'"));
    }
}
