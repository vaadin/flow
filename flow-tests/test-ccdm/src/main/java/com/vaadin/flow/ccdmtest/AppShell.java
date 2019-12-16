package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.server.PWA;

@Meta(name = "foo", content = "bar")
@PWA(name = "My App", shortName = "app")
@BodySize(height = "50vh", width = "50vw")
public class AppShell extends VaadinAppShell {
}
