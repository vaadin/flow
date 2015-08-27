package com.vaadin.tests.components.richtextarea;

import com.vaadin.event.UIEvents.PollEvent;
import com.vaadin.event.UIEvents.PollListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.RichTextArea;

public class RichTextAreaUpdateWhileTyping extends AbstractTestUI {

    private RichTextArea rta;

    @Override
    protected void setup(VaadinRequest request) {
        setPollInterval(1000);
        addPollListener(new PollListener() {

            @Override
            public void poll(PollEvent event) {
                rta.markAsDirty();
            }
        });

        rta = new RichTextArea();
        rta.setId("rta");

        add(rta);
    }

    @Override
    protected String getTestDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Integer getTicketNumber() {
        return 11741;
    }
}
