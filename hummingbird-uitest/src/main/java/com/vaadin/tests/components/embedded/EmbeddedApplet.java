package com.vaadin.tests.components.embedded;

import com.vaadin.server.ExternalResource;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

public class EmbeddedApplet extends TestBase {

    @Override
    protected String getTestDescription() {
        return "The sub window should be shown on top of the embedded applet";
    }

    @Override
    protected Integer getTicketNumber() {
        return 8399;
    }

    @Override
    public void setup() {
        final BrowserFrame applet = new BrowserFrame();
        applet.setWidth("400px");
        applet.setHeight("300px");
        applet.setSource(new ExternalResource("/statictestfiles/applet.html"));
        add(applet);

        add(new Button("Remove applet", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                remove(applet);
            }
        }));

    }
}
