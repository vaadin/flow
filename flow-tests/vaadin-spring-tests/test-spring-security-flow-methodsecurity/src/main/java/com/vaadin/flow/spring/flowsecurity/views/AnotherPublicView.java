package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "another", layout = MainView.class)
@PageTitle("Another Public View")
@AnonymousAllowed
public class AnotherPublicView extends FlexLayout {

    public AnotherPublicView() {
        setFlexDirection(FlexDirection.COLUMN);
        setHeightFull();

        H1 header = new H1("Another public view for testing");
        header.setId("header");
        header.getStyle().set("text-align", "center");
        add(header);
    }

}
