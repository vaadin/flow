package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.IFrameView", layout = ViewTestLayout.class)
public class IFrameView extends AbstractDivView {

    public IFrameView() {

        String top = "50%";
        String height = "50%";
        String height2 = "46%";
        String width = "50%";

        IFrame iFrame1 = new IFrame("/view/com.vaadin.flow.uitest.ui.PopupView");
        iFrame1.getElement().getStyle().set("position", "absolute");
        iFrame1.getElement().getStyle().set("border", "none");
        iFrame1.setWidth(width);
        iFrame1.setHeight(height2);

        add(iFrame1);

        IFrame iFrame2 = new IFrame("/view/com.vaadin.flow.uitest.ui.IFrameView");
        iFrame2.getElement().getStyle().set("position", "absolute");
        iFrame2.getElement().getStyle().set("left", width);
        iFrame2.setWidth(width);
        iFrame2.setHeight(height2);
        iFrame2.getElement().getStyle().set("border", "none");

        add(iFrame2);

        IFrame iFrame3 = new IFrame("/view/com.vaadin.flow.uitest.ui.PopupView");
        iFrame3.getElement().getStyle().set("position", "absolute");
        iFrame3.getElement().getStyle().set("top", top);
        iFrame3.getElement().getStyle().set("border", "none");
        iFrame3.setWidth(width);
        iFrame3.setHeight(height);
        iFrame3.setSandbox(IFrame.SandboxType.RESTRICT_ALL);

        add(iFrame3);

        Label label = new Label("Here I am");
        label.getElement().getStyle().set("position", "absolute");
        label.getElement().getStyle().set("top", top);
        label.getElement().getStyle().set("left", width);
        label.getElement().getStyle().set("border", "none");
        label.setWidth(width);
        label.setHeight(height);

        add(label);
    }
}
