package com.vaadin.flow.uitest.ui;

import java.util.Random;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.PreserveOnRefreshView")
@PreserveOnRefresh
public class PreserveOnRefreshView extends Div {

    public PreserveOnRefreshView() {
        // create unique content for this instance
        final String uniqueId = Long.toString(new Random().nextInt());

        setText(uniqueId);
        setId("contents");

        // also add an element as a separate UI child. This is expected to be
        // transferred on refresh (mimicking dialogs and notifications)
        final Element looseElement = new Element("div");
        looseElement.setProperty("id", "notification");
        looseElement.setText(uniqueId);
        UI.getCurrent().getElement().insertChild(0, looseElement);
    }

}
