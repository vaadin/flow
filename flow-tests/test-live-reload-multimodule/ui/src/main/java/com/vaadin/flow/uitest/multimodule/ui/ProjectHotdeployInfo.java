package com.vaadin.flow.uitest.multimodule.ui;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

@Tag("project-hotdeploy-info")
public class ProjectHotdeployInfo extends Div {

    public ProjectHotdeployInfo() {
        VaadinContext context = VaadinService.getCurrent().getContext();
        Span hotdeploy = new Span(ApplicationConfiguration.get(context)
                .getMode() == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD ? "true"
                        : "false");
        add(hotdeploy);
    }
}
