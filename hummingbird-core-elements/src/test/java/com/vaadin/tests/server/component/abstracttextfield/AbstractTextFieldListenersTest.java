package com.vaadin.tests.server.component.abstracttextfield;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.tests.server.component.AbstractListenerMethodsTestBase;
import com.vaadin.ui.TextField;

public class AbstractTextFieldListenersTest
        extends AbstractListenerMethodsTestBase {
    // public void testTextChangeListenerAddGetRemove() throws Exception {
    // testListenerAddGetRemove(TextField.class, TextChangeEvent.class,
    // TextChangeListener.class);
    // }

    public void testFocusListenerAddGetRemove() throws Exception {
        testDOMListenerAddGetRemove(TextField.class, FocusEvent.class,
                FocusListener.class);
    }

    public void testBlurListenerAddGetRemove() throws Exception {
        testDOMListenerAddGetRemove(TextField.class, BlurEvent.class,
                BlurListener.class);
    }
}
