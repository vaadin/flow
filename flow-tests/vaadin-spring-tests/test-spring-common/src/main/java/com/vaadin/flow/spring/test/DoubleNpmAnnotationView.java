/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

@JsModule("@polymer/paper-input/paper-input.js")
@JsModule("@polymer/paper-checkbox/paper-checkbox.js")
@Route("double-npm-annotation")
public class DoubleNpmAnnotationView extends Div {
    public DoubleNpmAnnotationView() {
        Element paperInput = new Element("paper-input");
        Element paperCheckbox = new Element("paper-checkbox");

        this.getElement().appendChild(paperInput, paperCheckbox);
    }
}
