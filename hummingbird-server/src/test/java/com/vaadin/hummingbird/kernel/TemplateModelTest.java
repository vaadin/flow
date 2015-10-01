package com.vaadin.hummingbird.kernel;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.Template;
import com.vaadin.ui.Template.Model;

public class TemplateModelTest {
    public interface MyTestModel extends Model {
        public String getValue();

        public void setValue(String value);
    }

    public class MyTestTemplate extends Template {
        public MyTestTemplate() {
            super(TemplateBuilder.withTag("foo").build());
        }

        @Override
        public MyTestModel getModel() {
            return (MyTestModel) super.getModel();
        }

        @Override
        public StateNode getNode() {
            // Override to make public
            return super.getNode();
        }
    }

    @Test
    public void testTemplateModelAccess() {
        MyTestTemplate template = new MyTestTemplate();
        MyTestModel model = template.getModel();
        StateNode node = template.getNode();

        model.setValue("foo");
        Assert.assertEquals("foo", node.get("value"));

        template.getNode().put("value", "bar");
        Assert.assertEquals("bar", model.getValue());
    }
}
