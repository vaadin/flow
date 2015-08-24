package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.ui.Template;

import elemental.json.Json;

public class TemplateComponentTest {
    private static class TestTemplateComponent extends Template {
        public List<Object> receivedValues = new ArrayList<>();

        public TestTemplateComponent() {
            super(BasicElementTemplate.get());
        }

        public void onBrowserEvent(String methodName, Object... params) {
            super.onBrowserEvent(getNode(), getElement(), methodName, params);
        }
    }

    @Test
    public void testSimpleHandlerMapping() {
        TestTemplateComponent template = new TestTemplateComponent() {
            @TemplateEventHandler
            private void withString(String value) {
                receivedValues.add(value);
            }
        };

        template.onBrowserEvent("withString", Json.create("My string"));

        Assert.assertEquals(Arrays.asList("My string"),
                template.receivedValues);
    }

    @Test
    public void testHandlerWithNodeParameter() {
        TestTemplateComponent template = new TestTemplateComponent() {
            @TemplateEventHandler
            private void withString(String value, StateNode node) {
                receivedValues.add(value);
                receivedValues.add(node);
            }
        };

        template.onBrowserEvent("withString", Json.create("My string"));

        Assert.assertEquals(
                Arrays.asList("My string", template.getElement().getNode()),
                template.receivedValues);
    }

    @Test(expected = RuntimeException.class)
    public void testMissingEventHandlerAnnotation() {
        TestTemplateComponent template = new TestTemplateComponent() {
            private void withString(String value) {
                receivedValues.add(value);
            }
        };

        template.onBrowserEvent("withString", Json.create("My string"));
    }

    @Test(expected = RuntimeException.class)
    public void testMissingEventHandlerMethod() {
        TestTemplateComponent template = new TestTemplateComponent() {
            // No method here
        };

        template.onBrowserEvent("withString", Json.create("My string"));
    }

}
