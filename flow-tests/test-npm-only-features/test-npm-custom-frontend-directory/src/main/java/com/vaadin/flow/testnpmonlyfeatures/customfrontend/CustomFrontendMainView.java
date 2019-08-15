package com.vaadin.flow.testnpmonlyfeatures.customfrontend;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.testnpmonlyfeatures.customfrontend.CustomFrontendMainView")
@JavaScript("./javascript.js")
public class CustomFrontendMainView extends Div {

    public CustomFrontendMainView() {
    }
}

