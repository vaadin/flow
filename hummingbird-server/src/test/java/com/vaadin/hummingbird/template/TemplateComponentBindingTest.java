package com.vaadin.hummingbird.template;

import com.vaadin.annotations.Id;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Template;

import org.junit.Assert;
import org.junit.Test;

public class TemplateComponentBindingTest {

    public static class TemplateWithFields extends Template {
        public Button button;

        @Id("button")
        public Button anotherButton;

        protected Grid grid;
        private Slider slider;
        private CssLayout cssLayout;

    }

    @Test
    public void fieldsPopulated() {
        TemplateWithFields template = new TemplateWithFields();
        Assert.assertNotNull(template.button);
        Assert.assertNotNull(template.anotherButton);
        Assert.assertNotNull(template.grid);
        Assert.assertNotNull(template.slider);
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
        Assert.assertEquals(template.grid.getParent(), template);
        Assert.assertEquals(template.slider.getParent(), template);
        Assert.assertEquals(template.cssLayout.getParent(), template);
    }

    @Test
    public void childrenCorrectForMappedComponents() {
        TemplateWithFields template = new TemplateWithFields();
        // <div id="cssLayout">
        // <button id="button" />
        // </div>
        // <vaadin-grid id="grid" />
        // <paper-slider id="slider" />
        Assert.assertArrayEquals(
                new Component[] { template.cssLayout, template.grid,
                        template.slider },
                template.getChildComponents().toArray());
        Assert.assertArrayEquals(new Component[] { template.button },
                template.cssLayout.getChildComponents().toArray());
    }

}
