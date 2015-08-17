package com.vaadin.tests.layouts;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.NativeButton;

public class CssLayoutCustomCss extends TestBase implements ClickListener {

    private CssLayout layout;

    @Override
    protected void setup() {
        setTheme("tests-tickets");
        layout = new CssLayout();
        layout.setSizeFull();
        addComponent(layout);

        Button red, green;
        layout.addComponent(red = createButton("color:red"));
        layout.addComponent(createButton("color: blue"));
        layout.addComponent(green = createButton("color: green"));

        red.click();
        green.click();
        layout.addComponent(createMarginsToggle());
    }

    private Component createMarginsToggle() {
        final CheckBox cb = new CheckBox("Margins");

        cb.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                ((ComponentContainer.MarginHandler) layout)
                        .setMargin(cb.getValue());
            }
        });

        return cb;
    }

    private Button createButton(String string) {
        NativeButton button = new NativeButton(string);
        applyStyle(button);
        button.addClickListener(this);
        return button;
    }

    @Override
    protected String getTestDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Integer getTicketNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void buttonClick(ClickEvent event) {
        Button b = event.getButton();
        if (b.getCaption().contains("not ")) {
            b.setCaption(b.getCaption().substring(4));
            applyStyle(b);
        } else {
            b.getElement().removeStyle("color");
            b.setCaption("not " + b.getCaption());
        }
        layout.markAsDirty();

    }

    private void applyStyle(Button b) {
        String[] keyValue = b.getCaption().split(":", 2);
        b.getElement().setStyle(keyValue[0], keyValue[1]);
    }

}
