package com.vaadin.hummingbird.template;

import com.vaadin.annotations.Id;
import com.vaadin.tests.server.TestButton;
import com.vaadin.tests.server.TestField;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Template;

import org.junit.Assert;
import org.junit.Test;

public class TemplateComponentBindingTest {

    public static class TemplateWithFields extends Template {
        public TestButton button;

        @Id("button")
        public TestButton anotherButton;

        private TestField field;
        private CssLayout cssLayout;

    }

    @Test
    public void fieldsPopulated() {
        TemplateWithFields template = new TemplateWithFields();
        Assert.assertNotNull(template.button);
        Assert.assertNotNull(template.anotherButton);
        Assert.assertNotNull(template.field);
        Assert.assertNotNull(template.cssLayout);
    }

    @Test
    public void multipleComponentsWithTheSameElement() {
        TemplateWithFields template = new TemplateWithFields();
        Assert.assertSame(template.button, template.anotherButton);
    }

    @Test
    public void parentCorrectForMappedComponents() {
        TemplateWithFields template = new TemplateWithFields();
        Assert.assertEquals(template.button.getParent(), template.cssLayout);
        Assert.assertEquals(template.field.getParent(), template);
        Assert.assertEquals(template.cssLayout.getParent(), template);
    }

    @Test
    public void childrenCorrectForMappedComponents() {
        TemplateWithFields template = new TemplateWithFields();
        // <div id="css-layout">
        // <button id="button" />
        // </div>
        // <div id="testcomponent" />
        // <input id="field" />
        Assert.assertArrayEquals(
                new Component[] { template.cssLayout, template.field },
                template.getChildComponents().toArray());
        Assert.assertArrayEquals(new Component[] { template.button },
                template.cssLayout.getChildComponents().toArray());
    }

}
