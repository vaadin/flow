package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.testnpmonlyfeatures.nobuildmojo.NoAppBundleView")
public class NoAppBundleView extends Div {

    public NoAppBundleView() {
        Span hello = new Span("Hello");
        hello.setId("hello-component");
        add(hello);
    }
}
