/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.ScriptInjectView", layout = ViewTestLayout.class)
public class ScriptInjectView extends AbstractDivView {

    static String[] values = new String[] { "</script foo>", "</Script>",
            "</SCRIPT >", "</SCRIPT>", "< / SCRIPT>", "</ SCRIPT>",
            "< / SCRIPT >", "</SCRIpT>" };

    public ScriptInjectView() {
        for (String value : values) {
            createInput(value);
        }
    }

    private void createInput(String endTag) {
        String string = getValue(endTag);
        Element input = ElementFactory.createInput();
        input.setAttribute("value", string);
        getElement().appendChild(input);
    }

    static String getValue(String endTag) {
        return endTag + "<script>alert('foo');>" + endTag;
    }
}
