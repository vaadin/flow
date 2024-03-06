/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Abstract layout to test various resourcess accessibility. Contains:
 * <ul>
 * <li>Static CSS</li>
 * <li>Dynamically loadable CSS.</li>
 * <li>Static JS script</li>
 * <li>Dynamically loadable JS script.</li>
 * </ul>
 *
 * @since 1.2
 */
@StyleSheet("context://test-files/css/allred.css")
public abstract class DependencyLayout extends Div {

    public static final String RUN_PUSH_ID = "runPush";
    public static final String PUSH_SIGNAL_ID = "push-signal";
    public static final String PUSH_WORKS_TEXT = "Push works";
    public static final String NO_PUSH_YET_TEXT = "No Push Yet";
    private final Element pushWorks;

    @StyleSheet("context://test-files/css/allblueimportant.css")
    public static class AllBlueImportantComponent extends Div {

        public AllBlueImportantComponent() {
            setText("allblueimportant.css component");
        }

    }

    @JavaScript("./test-files/js/body-click-listener.js")
    public static class JsResourceComponent extends Div {

        public JsResourceComponent() {
            setText("Hello, click the body please");
            setId("hello");
        }
    }

    public DependencyLayout() {
        getElement().appendChild(ElementFactory.createDiv(
                "This test initially loads a stylesheet which makes all text red and a JavaScript which listens to body clicks"));
        getElement().appendChild(ElementFactory.createHr());
        add(new JsResourceComponent());

        Element jsOrder = ElementFactory.createButton("Load js")
                .setAttribute("id", "loadJs");
        StreamRegistration jsStreamRegistration = VaadinSession.getCurrent()
                .getResourceRegistry().registerResource(getJsResource());
        jsOrder.addEventListener("click", event -> {
            UI.getCurrent().getPage().addJavaScript("base://"
                    + jsStreamRegistration.getResourceUri().toString());
        });
        Element allBlue = ElementFactory
                .createButton("Load 'everything blue' stylesheet")
                .setAttribute("id", "loadBlue");
        allBlue.addEventListener("click", event -> {
            add(new AllBlueImportantComponent());

        });

        Element runPush = ElementFactory
                .createButton("Run delayed push request")
                .setAttribute("id", RUN_PUSH_ID);

        pushWorks = ElementFactory.createDiv(NO_PUSH_YET_TEXT);
        pushWorks.setAttribute("id", PUSH_SIGNAL_ID);
        runPush.addEventListener("click", event -> {
            UI ui = getUI().orElseThrow(IllegalStateException::new);
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        ui.access(() -> {
                            try {
                                // if push does not work, we'll fail here
                                ui.push();
                            } catch (Throwable e) {
                                LoggerFactory.getLogger(DependencyLayout.class)
                                        .debug("Push does not work (most probably not a problem)",
                                                e);
                                return;
                            }
                            pushWorks.setText(PUSH_WORKS_TEXT);
                            ui.push();

                        });

                    } catch (InterruptedException ignored) {

                    }
                }
            }.start();
        });
        getElement().appendChild(jsOrder, allBlue, runPush,
                ElementFactory.createHr(), pushWorks);
    }

    private StreamResource getJsResource() {
        StreamResource jsRes = new StreamResource("element-appender.js", () -> {
            String js = "var div = document.createElement('div');"
                    + "div.id = 'appended-element';"
                    + "div.textContent = 'Added by script';"
                    + "document.body.appendChild(div, null);";

            // Wait to ensure that client side will stop until the JavaScript is
            // loaded
            try {
                Thread.sleep(500);
            } catch (Exception ignored) {
            }
            return new ByteArrayInputStream(
                    js.getBytes(StandardCharsets.UTF_8));
        });
        return jsRes;
    }

}
