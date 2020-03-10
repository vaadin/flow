package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;

public abstract class SendMultibyteCharactersUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        Push push = getClass().getAnnotation(Push.class);

        getPushConfiguration().setPushMode(push.value());
        getPushConfiguration().setTransport(push.transport());

        Div div = new Div();
        div.setText("Just a label");
        div.setId("label");
        add(div);
        Element area = ElementFactory.createTextarea();
        area.addPropertyChangeListener("value", "change", event -> {
        });
        area.setAttribute("id", "text");
        getElement().appendChild(area);
    }

}
