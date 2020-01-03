/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

public class TestingServiceInitListener implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addDependencyFilter((dependencies, context) -> {
            // used by DependencyFilterUI
            if (dependencies.stream().anyMatch(dependency -> dependency.getUrl()
                    .startsWith("replaceme://"))) {
                List<Dependency> newList = new ArrayList<>();
                newList.add(new Dependency(Dependency.Type.HTML_IMPORT,
                        "frontend://com/vaadin/flow/uitest/ui/dependencies/filtered.html",
                        LoadMode.EAGER));
                dependencies.stream()
                        .filter(dependency -> !dependency.getUrl()
                                .startsWith("replaceme://"))
                        .forEach(newList::add);
                dependencies = newList;
            }
            // used by BundledTemplateInTemplateWithIdView
            else if (dependencies.stream().anyMatch(dependency -> dependency
                    .getUrl().startsWith("bundle://"))) {
                List<Dependency> newList = new ArrayList<>();
                newList.add(new Dependency(Dependency.Type.HTML_IMPORT,
                        "frontend://com/vaadin/flow/uitest/ui/template/BundleIdTemplate.html",
                        LoadMode.EAGER));
                dependencies = newList;
            }
            return dependencies;
        });
    }

}
