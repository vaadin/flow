package com.vaadin.tests.components.notification;

import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Page;
import com.vaadin.ui.TextArea;

public class Notifications extends TestBase implements ClickListener {

    private static final String CAPTION = "CAPTION";
    private TextArea tf;
    private NativeSelect type;

    @SuppressWarnings("deprecation")
    @Override
    protected void setup() {
        tf = new TextArea("Text", "Hello world");
        tf.setRows(10);
        add(tf);
        type = new NativeSelect();
        type.setNullSelectionAllowed(false);
        type.addContainerProperty(CAPTION, String.class, "");
        type.setItemCaptionPropertyId(CAPTION);
        type.addItem(Type.HUMANIZED_MESSAGE).getItemProperty(CAPTION)
                .setValue("Humanized");
        type.addItem(Type.ERROR_MESSAGE).getItemProperty(CAPTION)
                .setValue("Error");
        type.addItem(Type.WARNING_MESSAGE).getItemProperty(CAPTION)
                .setValue("Warning");
        type.addItem(Type.TRAY_NOTIFICATION).getItemProperty(CAPTION)
                .setValue("Tray");
        type.setValue(type.getItemIds().iterator().next());
        add(type);
        Button showNotification = new Button("Show notification", this);
        add(showNotification);
    }

    @Override
    protected String getTestDescription() {
        return "Generic test case for notifications";
    }

    @Override
    protected Integer getTicketNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void buttonClick(ClickEvent event) {
        Notification n = new Notification(tf.getValue(),
                (Type) type.getValue());
        n.show(Page.getCurrent());
    }
}
