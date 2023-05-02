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
