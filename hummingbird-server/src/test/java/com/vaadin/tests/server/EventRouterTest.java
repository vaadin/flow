package com.vaadin.tests.server;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.TestField;

import junit.framework.TestCase;

public class EventRouterTest extends TestCase {

    int innerListenerCalls = 0;

    public void testAddInEventListener() {
        final TestField tf = new TestField();

        final ValueChangeListener outer = new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                ValueChangeListener inner = new ValueChangeListener() {

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        innerListenerCalls++;
                        System.out.println("The inner listener was called");
                    }
                };

                tf.addValueChangeListener(inner);
            }
        };

        tf.addValueChangeListener(outer);
        tf.setValue("abc"); // No inner listener calls, adds one inner
        tf.setValue("def"); // One inner listener call, adds one inner
        tf.setValue("ghi"); // Two inner listener calls, adds one inner
        assert (innerListenerCalls == 3);
    }
}
