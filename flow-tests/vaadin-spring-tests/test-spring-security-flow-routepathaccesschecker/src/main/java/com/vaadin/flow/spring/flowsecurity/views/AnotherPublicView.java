package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@Route(value = "another", layout = MainView.class)
@RouteAlias("hey/:name/welcome/:wild*")
@PageTitle("Another Public View")
public class AnotherPublicView extends FlexLayout
        implements BeforeEnterObserver {

    private final Span name;
    private final Span wild;

    public AnotherPublicView() {
        setFlexDirection(FlexDirection.COLUMN);
        setHeightFull();

        H1 header = new H1("Another public view for testing");
        header.setId("header");
        header.getStyle().set("text-align", "center");
        add(header);
        add(new Anchor("hey/anchor/welcome/home", "Link to alias"));

        name = new Span();
        name.setId("p-name");

        wild = new Span();
        wild.setId("p-wild");
        add(name, wild);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        name.setText(event.getRouteParameters().get("name").orElse("-"));
        wild.setText(event.getRouteParameters().get("wild").orElse("-"));
    }

}
