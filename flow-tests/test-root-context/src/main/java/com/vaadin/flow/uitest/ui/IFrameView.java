package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Iframe;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.IFrameView", layout = ViewTestLayout.class)
public class IFrameView extends AbstractDivView {

    public IFrameView() {

        String top = "20%";
        String height = "80%";

        Iframe iFrame = new Iframe("https://www.vaadin.com");
        iFrame.getElement().getStyle().set("position", "absolute");
        iFrame.getElement().getStyle().set("top", top);
        iFrame.getElement().getStyle().set("width", "50%");
        iFrame.getElement().getStyle().set("height", height);

        add(iFrame);

        Iframe iFrame2 = new Iframe("http://localhost:8080");
        iFrame2.getElement().getStyle().set("position", "absolute");
        iFrame2.getElement().getStyle().set("top", top);
        iFrame2.getElement().getStyle().set("left", "50%");
        iFrame2.getElement().getStyle().set("width", "50%");
        iFrame2.getElement().getStyle().set("height", height);

        add(iFrame2);

        Iframe iFrame3 = new Iframe("https://www.bing.com");
        iFrame3.getElement().getStyle().set("position", "absolute");
        iFrame3.getElement().getStyle().set("width", "100%");
        iFrame3.getElement().getStyle().set("height", "15%");

        add(iFrame3);

    }
}
