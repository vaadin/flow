package com.vaadin.flow.addon;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.select.SelectElement;

@Route(value = "com.vaadin.flow.addon.AddOnView")
public class AddOnView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        SelectElement select = new SelectElement("one", "two");
        add(select);
    }

}
