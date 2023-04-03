package com.vaadin.flow.uitest.ui;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ThemeLiveReloadView", layout = ViewTestLayout.class)
public class ThemeLiveReloadView extends AbstractLiveReloadView {

    public ThemeLiveReloadView() {
        ApplicationConfiguration appConf = ApplicationConfiguration
                .get(VaadinService.getCurrent().getContext());
        Path stylesPath = Paths.get(
                appConf.getProjectFolder().getAbsolutePath(), "frontend",
                "themes", "mytheme", "styles.css");
        Span span = new Span(stylesPath.toString());
        span.setId("styles.css");
        add(span);
        Div div1 = new Div();
        div1.setId("div1");
        div1.setText("This is div 1, it has a lightgreen background");
        add(div1);
        Div div2 = new Div();
        div2.setId("div2");
        div2.setText("This is div 2, it has a blue background and white text");
        add(div2);
    }

}
