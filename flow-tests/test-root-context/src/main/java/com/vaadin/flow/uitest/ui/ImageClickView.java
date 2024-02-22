package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ImageClickView", layout = ViewTestLayout.class)
public class ImageClickView extends AbstractDivView {

    int count1 = 0;
    int count2 = 0;
    int count3 = 0;

    public ImageClickView() {
        Div message = new Div();
        message.setText("Before click");
        message.setId("message");
        Div message2 = new Div();
        message2.setId("message2");
        Div message3 = new Div();
        message3.setId("message3");

        Image image = new Image("", "IMAGE");
        image.setId("image");
        image.addClickListener(event -> {
            count1++;
            message.setText("After click " + count1);
        });
        image.addSingleClickListener(event -> {
            count2++;
            message2.setText("Single click " + count2);
        });
        image.addDoubleClickListener(event -> {
            count3++;
            message3.setText("Double click " + count3);
        });
        add(image, message, message2, message3);
    }
}
