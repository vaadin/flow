package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeDetails;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

@Route("Details")
public class NativeDetailsView extends Div {

    public NativeDetailsView() {
        Div log = new Div();
        log.setId("log");

        NativeDetails details = new NativeDetails("summary", new Paragraph("content"));
        details.setId("details");
        details.addOpenChangedListener(e -> {
            log.setText("Toggle event is '" + e.isOpened() + "'");
        });
        add(log, details);
    }
}
