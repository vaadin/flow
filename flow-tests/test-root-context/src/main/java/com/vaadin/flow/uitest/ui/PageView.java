package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEnterEvent;

@Route(value = "com.vaadin.flow.uitest.ui.PageView", layout = ViewTestLayout.class)
public class PageView extends AbstractDivView {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        Input input = new Input();
        input.setId("input");
        input.clear();

        Div updateButton = new Div();
        updateButton.setId("button");
        updateButton.setText("Update page title");
        updateButton.addClickListener(e -> {
            getPage().setTitle(input.getValue());
        });

        Div overrideButton = new Div();
        overrideButton.setId("override");
        overrideButton.setText("Triggers two updates");
        overrideButton.addClickListener(e -> {
            getPage().setTitle(input.getValue());
            getPage().setTitle("OVERRIDDEN");
        });

        Div reloadButton = new Div();
        reloadButton.setId("reload");
        reloadButton.setText("Reloads the page");
        reloadButton.addClickListener(e -> {
            getPage().reload();
        });

        String url = "https://www.google.com";

        Div setLocationButton = new Div();
        setLocationButton.setId("setLocation");
        setLocationButton.setText("Set page location");
        setLocationButton.addClickListener(e -> getPage().setLocation(url));

        Div openButton = new Div();
        openButton.setId("open");
        openButton.setText("Open url");
        openButton.addClickListener(e -> getPage().open(url, "secondwindow"));

        Div openWithSizeButton = new Div();
        openWithSizeButton.setId("openWithSize");
        openWithSizeButton.setText("Open url in a window with height and width");
        openWithSizeButton.addClickListener(e -> {
            getPage().open(url, "sizewindow", 400, 400);
        });

        add(input, updateButton, overrideButton, reloadButton,
                setLocationButton, openButton, openWithSizeButton);
    }

}
