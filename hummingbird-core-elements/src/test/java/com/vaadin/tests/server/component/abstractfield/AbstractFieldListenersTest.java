package com.vaadin.tests.server.component.abstractfield;

import com.vaadin.data.Property.ReadOnlyStatusChangeEvent;
import com.vaadin.data.Property.ReadOnlyStatusChangeListener;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.tests.server.TestField;
import com.vaadin.tests.server.component.AbstractListenerMethodsTestBase;
import com.vaadin.ui.TextField;

public class AbstractFieldListenersTest
        extends AbstractListenerMethodsTestBase {
    public void testReadOnlyStatusChangeListenerAddGetRemove()
            throws Exception {
        testListenerAddGetRemove(TextField.class,
                ReadOnlyStatusChangeEvent.class,
                ReadOnlyStatusChangeListener.class);
    }

    public void testValueChangeListenerAddGetRemove() throws Exception {
        testListenerAddGetRemove(TestField.class, ValueChangeEvent.class,
                ValueChangeListener.class);
    }
}
