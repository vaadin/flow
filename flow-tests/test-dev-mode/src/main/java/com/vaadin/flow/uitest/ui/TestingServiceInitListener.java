/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
