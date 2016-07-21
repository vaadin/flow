package com.vaadin.hummingbird.uitest.ui.template;

import com.vaadin.annotations.Id;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Template;

public class TemplateElementEncodingView extends Template {

    @Id("span")
    private Element span;

    @Id("label")
    private Element label;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        label.setText("foobar");

        attachEvent.getUI().getPage().executeJavaScript(
                "var x = document.createElement('div');" + "x.id = 'result';"
                        + "x.textContent = $0.id + ' ' + $1.id + ' ' + $2.textContent;"
                        + "$0.appendChild(x);",
                getElement(), span, label);
    }
}
