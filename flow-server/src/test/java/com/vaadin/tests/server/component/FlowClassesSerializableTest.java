package com.vaadin.tests.server.component;

import java.io.OutputStream;

import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.testutil.ClassesSerializableTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class FlowClassesSerializableTest extends ClassesSerializableTest {

    /**
     * {@link HtmlComponent} and {@link HtmlContainer} are not covered by
     * generic test because of their constructors
     */
    @Test
    public void htmlComponentAndHtmlContainer() throws Throwable {
        Component[] components = {new HtmlComponent("dummy-tag"),
                new HtmlContainer("dummy-tag")};
        for (Component component : components) {
            Component componentCopy = serializeAndDeserialize(component);
            assertEquals(component.getElement().getTag(), componentCopy.getElement().getTag());
            assertNotSame(component.getElement(), componentCopy.getElement());
        }
    }

    /**
     * Tests a serialization bug (probably located in JVM ) when serialized
     * {@link Command} is deserialized as some internal lambda and produces
     * {@link ClassCastException}
     */
    @Test
    public void streamResource() throws Throwable {
        UI.setCurrent(new UI());
        try {
            Element element = new Element("dummy-element");
            StreamReceiver streamReceiver = new StreamReceiver(
                    element.getNode(), "upload", new MyStreamVariable());
            element.setAttribute("target", streamReceiver);
            serializeAndDeserialize(element);
            assertTrue("Basic smoke test with ", element.getAttribute("target").length() > 10);
        } finally {
            UI.setCurrent(null);
        }
    }

    private static class MyStreamVariable implements StreamVariable {
        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        public boolean listenProgress() {
            return false;
        }

        @Override
        public void onProgress(StreamingProgressEvent event) {

        }

        @Override
        public void streamingStarted(StreamingStartEvent event) {

        }

        @Override
        public void streamingFinished(StreamingEndEvent event) {

        }

        @Override
        public void streamingFailed(StreamingErrorEvent event) {

        }

        @Override
        public boolean isInterrupted() {
            return false;
        }
    }
}
