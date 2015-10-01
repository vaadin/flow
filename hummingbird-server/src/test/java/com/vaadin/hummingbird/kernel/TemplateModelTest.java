package com.vaadin.hummingbird.kernel;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.Template;
import com.vaadin.ui.Template.Model;

public class TemplateModelTest {
    public interface MyTestModel extends Model {
        public String getValue();

        public void setValue(String value);

        public boolean isBoolean();

        public void setBoolean(boolean value);
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

    private MyTestTemplate template = new MyTestTemplate();
    private MyTestModel model = template.getModel();
    private StateNode node = template.getNode();

    @Test
    public void testTemplateModelAccess() {
        model.setValue("foo");
        Assert.assertEquals("foo", node.get("value"));

        template.getNode().put("value", "bar");
        Assert.assertEquals("bar", model.getValue());
    }

    @Test
    public void testBooleanFalseRemovesProperty() {
        Assert.assertFalse(model.isBoolean());

        model.setBoolean(true);
        Assert.assertEquals(Boolean.TRUE, node.get("boolean"));

        model.setBoolean(false);
        Assert.assertFalse(node.containsKey("boolean"));
    }
}
