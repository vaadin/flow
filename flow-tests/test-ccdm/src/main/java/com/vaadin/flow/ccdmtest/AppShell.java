package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;

@Meta(name = "foo", content = "bar")
@PWA(name = "My App", shortName = "app")
@Viewport(Viewport.DEVICE_DIMENSIONS)
public class AppShell extends VaadinAppShell {
}
