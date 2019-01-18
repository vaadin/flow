package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ImageClickView", layout = ViewTestLayout.class)
public class ImageClickView extends AbstractDivView {

    public ImageClickView() {
        Div message = new Div();
        message.setText("Before click");
        message.setId("message");

        Image image = new Image("", "IMAGE");
        image.setId("image");
        image.addClickListener(event -> message.setText("After click"));
        add(image, message);
    }
}
