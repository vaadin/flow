package com.vaadin.flow.uitest.ui;

import java.util.Random;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.PreserveOnRefreshView")
@PreserveOnRefresh
public class PreserveOnRefreshView extends Div {

    final static String COMPONENT_ID = "contents";
    final static String NOTIFICATION_ID = "notification";
    final static String ATTACHCOUNTER_ID = "attachcounter";

    private int attached = 0;
    private final Div attachCounter;

    public PreserveOnRefreshView() {
        // create unique content for this instance
        final String uniqueId = Long.toString(new Random().nextInt());

        final Div componentId = new Div();
        componentId.setId(COMPONENT_ID);
        componentId.setText(uniqueId);
        add(componentId);

        // add an element to keep track of number of attach events
        attachCounter = new Div();
        attachCounter.setId(ATTACHCOUNTER_ID);
        attachCounter.setText("0");
        add(attachCounter);

        // also add an element as a separate UI child. This is expected to be
        // transferred on refresh (mimicking dialogs and notifications)
        final Element looseElement = new Element("div");
        looseElement.setProperty("id", NOTIFICATION_ID);
        looseElement.setText(uniqueId);
        UI.getCurrent().getElement().insertChild(0, looseElement);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        attached += 1;
        attachCounter.setText(Integer.toString(attached));
    }
}
