package com.vaadin.tests.server.component.select;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.tests.server.component.AbstractListenerMethodsTestBase;
import com.vaadin.ui.ComboBox;

public class SelectListenersTest extends AbstractListenerMethodsTestBase {
    public void testFocusListenerAddGetRemove() throws Exception {
        testListenerAddGetRemove(ComboBox.class, FocusEvent.class, FocusListener.class);
    }

    public void testBlurListenerAddGetRemove() throws Exception {
        testListenerAddGetRemove(ComboBox.class, BlurEvent.class, BlurListener.class);
    }
}
