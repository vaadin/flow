package com.vaadin.tests.server.component;

import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.testutil.ClassesSerializableTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

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

}
