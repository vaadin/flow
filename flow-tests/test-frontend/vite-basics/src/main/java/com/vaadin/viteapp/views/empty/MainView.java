package com.vaadin.viteapp.views.empty;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends Div {

    public MainView() {
        Image img = new Image("themes/vite-basics/images/plant.png",
                "placeholder plant");
        img.setWidth("200px");
        add(img);

        add(new H2("This place intentionally left empty"));
        add(new Paragraph("It’s a place where you can grow your own UI 🤗"));

        NativeButton button = new NativeButton("Show/hide plant", e -> {
            img.setVisible(!img.isVisible());
        });
        add(button);
        setSizeFull();
        getStyle().set("text-align", "center");
    }

}
