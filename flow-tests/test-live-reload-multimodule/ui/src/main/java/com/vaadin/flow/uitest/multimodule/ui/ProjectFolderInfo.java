/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.uitest.multimodule.ui;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

@Tag("project-folder-info")
public class ProjectFolderInfo extends Div {

    public ProjectFolderInfo() {
        VaadinContext context = VaadinService.getCurrent().getContext();
        Span info = new Span(ApplicationConfiguration.get(context)
                .getProjectFolder().getAbsolutePath());
        add(info);
    }
}
