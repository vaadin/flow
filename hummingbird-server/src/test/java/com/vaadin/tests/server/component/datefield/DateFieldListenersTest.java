package com.vaadin.tests.server.component.datefield;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.tests.server.component.AbstractListenerMethodsTestBase;
import com.vaadin.ui.DateField;

public class DateFieldListenersTest extends AbstractListenerMethodsTestBase {
    public void testFocusListenerAddGetRemove() throws Exception {
        testDOMListenerAddGetRemove(DateField.class, FocusEvent.class,
                FocusListener.class);
    }

    public void testBlurListenerAddGetRemove() throws Exception {
        testDOMListenerAddGetRemove(DateField.class, BlurEvent.class,
                BlurListener.class);
    }
}
