package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "", layout = MainView.class)
@PageTitle("Public View")
@AnonymousAllowed
public class PublicView extends FlexLayout {

    public PublicView() {
        setFlexDirection(FlexDirection.COLUMN);
        setHeightFull();

        H1 header = new H1("Welcome to the Java Bank of Vaadin");
        header.setId("header");
        header.getStyle().set("text-align", "center");
        add(header);
        Image image = new Image("public/images/bank.jpg", "Bank");
        image.getStyle().set("max-width", "100%").set("min-height", "0");
        add(image);
        add(new Paragraph(
                "We are very great and have great amounts of money."));
    }

}
