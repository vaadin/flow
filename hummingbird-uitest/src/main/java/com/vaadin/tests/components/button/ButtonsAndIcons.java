package com.vaadin.tests.components.button;

import com.vaadin.server.ExternalResource;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Button;
import com.vaadin.ui.NativeButton;

public class ButtonsAndIcons extends TestBase {

    @Override
    protected String getTestDescription() {
        return "The first button has text and an icon, the second only text and the third only an icon.";
    }

    @Override
    protected Integer getTicketNumber() {
        return 3031;
    }

    @Override
    protected void setup() {
        Button b = new Button("Text and icon");
        b.setIcon(new ExternalResource("vaadin://themes/runo/icons/16/ok.png"));

        add(b);

        b = new Button("Only text");

        add(b);
        b = new Button((String) null);
        b.setIcon(new ExternalResource("vaadin://themes/runo/icons/16/ok.png"));

        add(b);

        b = new NativeButton("Text and icon");
        b.setIcon(new ExternalResource("vaadin://themes/runo/icons/16/ok.png"));

        add(b);

        b = new NativeButton("Only text");

        add(b);
        b = new NativeButton(null);
        b.setIcon(new ExternalResource("vaadin://themes/runo/icons/16/ok.png"));

        add(b);
    }

}
