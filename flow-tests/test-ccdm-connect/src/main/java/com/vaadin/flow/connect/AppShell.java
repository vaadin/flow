package com.vaadin.flow.connect;

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.server.PWA;

@Meta(name = "foo", content = "bar")
@PWA(name = "My App", shortName = "app")
public class AppShell extends VaadinAppShell {
}
