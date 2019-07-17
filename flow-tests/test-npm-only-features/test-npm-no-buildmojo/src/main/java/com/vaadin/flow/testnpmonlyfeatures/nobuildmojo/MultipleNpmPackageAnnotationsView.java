package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;


@JsModule("@polymer/paper-input/paper-input.js")
@JsModule("@polymer/paper-checkbox/paper-checkbox.js")
@Route(value = "com.vaadin.flow.testnpmonlyfeatures.nobuildmojo" +
        ".MultipleNpmPackageAnnotationsView", layout = NpmPackageLayout.class)
@JsModule("./connector.js")
public class MultipleNpmPackageAnnotationsView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        Element paperInput = new Element("paper-input");
        paperInput.setText("Input");
        Element paperCheckbox = new Element("paper-checkbox");
        paperCheckbox.setText("Checkbox");

        getElement().appendChild(paperInput);
        getElement().appendChild(paperCheckbox);

        initConnector();
    }

    private void initConnector() {
        getElement().executeJs("window.Vaadin.Flow.connector.initLazy()");
    }
}
