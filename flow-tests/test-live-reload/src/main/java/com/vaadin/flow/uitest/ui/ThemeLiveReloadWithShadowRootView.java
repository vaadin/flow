package com.vaadin.flow.uitest.ui;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ThemeLiveReloadWithShadowRootView", layout = ViewTestLayout.class)
@JsModule("./component-with-theme.ts")
public class ThemeLiveReloadWithShadowRootView extends AbstractLiveReloadView {

    public ThemeLiveReloadWithShadowRootView() {
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
        ComponentWithTheme c2 = new ComponentWithTheme();
        c2.setId("component-with-theme");
        add(c2);
    }

}
