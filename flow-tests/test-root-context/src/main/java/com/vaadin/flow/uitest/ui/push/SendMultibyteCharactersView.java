package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public abstract class SendMultibyteCharactersView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        CustomPush push = getClass().getAnnotation(CustomPush.class);

        attachEvent.getUI().getPushConfiguration().setPushMode(push.value());
        attachEvent.getUI().getPushConfiguration()
                .setTransport(push.transport());

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
