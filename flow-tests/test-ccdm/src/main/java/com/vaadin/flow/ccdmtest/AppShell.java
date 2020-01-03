package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinService;

@Meta(name = "foo", content = "bar")
@PWA(name = "My App", shortName = "app")
@Viewport(Viewport.DEVICE_DIMENSIONS)
@BodySize(height = "50vh", width = "50vw")
public class AppShell implements AppShellConfigurator {
    private final String url;

    public AppShell() {
        url = VaadinService.getCurrent().resolveResource("my-resource");
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addMetaTag("test-resource-url", url);
    }
}
