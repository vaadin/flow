package com.vaadin.flow.component;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.ComponentTest.TracksAttachDetach;

public class CompositeTextNodeTest extends CompositeTest {

    static class TracksAttachDetachText extends Text
            implements TracksAttachDetach {

        public TracksAttachDetachText(String text) {
            super(text);
        }

        private AtomicInteger attachEvents = new AtomicInteger();
        private AtomicInteger detachEvents = new AtomicInteger();

        @Override
        public AtomicInteger getAttachEvents() {
            return attachEvents;
        }

        @Override
        public AtomicInteger getDetachEvents() {
            return detachEvents;
        }

    }

    @Override
    protected Component createTestComponent() {
        return new TracksAttachDetachText("Test component");
    }
}
