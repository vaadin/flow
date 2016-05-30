package com.vaadin.hummingbird.template.model;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.ui.Template;

public class TemplateModelTest {

    public interface EmptyModel extends TemplateModel {

    }

    public interface BasicTypeModel extends TemplateModel {
        public boolean getBooleanPrimitive();

        public boolean isBooleanPrimitive();

        public void setBooleanPrimitive(boolean b);

        public Boolean getBoolean();

        public void setBoolean(Boolean b);

        public int getInt();

        public void setInt(int i);

        public Integer getInteger();

        public void setInteger(Integer i);

        public double getDoublePrimitive();

        public void setDoublePrimitive(double d);

        public Double getDouble();

        public void setDouble(Double d);

        public String getString();

        public void setString(String s);
    }

    public interface NotSupportedModel extends TemplateModel {
        public void setLong(long l);

        public long getLong();

        public void setBean(Bean b);

        public Bean getBean();

        public void setFoo();

        public int setFoo(int foo);

        public int getFoo(int foo);

        public void getFoo();

        public void setFoo(int x, int y);

        public void setfoo(int i);

        public int isbar();
    }

    public static class Bean implements Serializable {
    }

    static class NoModelTemplate extends Template {
        public NoModelTemplate() {
            super(new ByteArrayInputStream(
                    "<div>foo</div>".getBytes(StandardCharsets.UTF_8)));
        }

    }

    static class EmptyModelTemplate extends NoModelTemplate {
        @Override
        protected EmptyModel getModel() {
            return (EmptyModel) super.getModel();
        }
    }

    static class BasicTypeModelTemplate extends NoModelTemplate {
        @Override
        protected BasicTypeModel getModel() {
            return (BasicTypeModel) super.getModel();
        };
    }

    static class NotSupportedModelTemplate extends NoModelTemplate {
        @Override
        protected NotSupportedModel getModel() {
            return (NotSupportedModel) super.getModel();
        }
    }

    @Test
    public void testTemplateModelTypeReading() {
        Class<? extends TemplateModel> templateModelType = TemplateModelTypeParser
                .getType(NoModelTemplate.class);

        Assert.assertEquals(TemplateModel.class, templateModelType);

        templateModelType = TemplateModelTypeParser
                .getType(EmptyModelTemplate.class);

        Assert.assertEquals(EmptyModel.class, templateModelType);
    }

    @Test
    public void testTemplateModelTypeCache() {
        TemplateModelTypeParser.cache.clear();

        Class<? extends TemplateModel> first = TemplateModelTypeParser
                .getType(EmptyModelTemplate.class);
        Class<? extends TemplateModel> second = TemplateModelTypeParser
                .getType(EmptyModelTemplate.class);

        Assert.assertSame(first, second);
    }

    @Test
    public void testTemplateModelCreation() {
        EmptyModelTemplate emptyModelTemplate = new EmptyModelTemplate();

        TemplateModel modelProxy = emptyModelTemplate.getModel();
        TemplateModel modelProxy2 = emptyModelTemplate.getModel();

        Assert.assertTrue(Proxy.isProxyClass(modelProxy.getClass()));
        Assert.assertTrue(modelProxy == modelProxy2);

        modelProxy2 = new EmptyModelTemplate().getModel();
        Assert.assertTrue(Proxy.isProxyClass(modelProxy2.getClass()));
        Assert.assertNotSame(modelProxy, modelProxy2);
    }

    @Test
    public void testSetterSameValue_noUpdates() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        model.setString("foobar");

        Assert.assertEquals("foobar", model.getString());

        ArrayList<NodeChange> changes = new ArrayList<>();
        ModelMap modelMap = template.getElement().getNode()
                .getFeature(ModelMap.class);
        modelMap.collectChanges(changes::add);

        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(template.getElement().getNode(),
                changes.get(0).getNode());

        changes.clear();
        template.getElement().getNode().clearChanges();

        model.setString("foobar");

        Assert.assertEquals("foobar", model.getString());
        modelMap.collectChanges(changes::add);

        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testBooleanValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getBoolean());

        model.setBoolean(Boolean.TRUE);

        Assert.assertEquals(Boolean.TRUE, model.getBoolean());

        model.setBoolean(Boolean.FALSE);

        Assert.assertEquals(Boolean.FALSE, model.getBoolean());

        model.setBoolean(null);

        Assert.assertEquals(null, model.getBoolean());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBooleanParseString() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getBoolean());

        template.getElement().getNode().getFeature(ModelMap.class)
                .setValue("boolean", "True");

        model.getBoolean();
    }

    @Test(expected = ClassCastException.class)
    public void testBooleanPrimitiveParseString() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(Boolean.FALSE, model.isBooleanPrimitive());

        template.getElement().getNode().getFeature(ModelMap.class)
                .setValue("booleanPrimitive", "TRUE");

        model.getBooleanPrimitive();
    }

    @Test
    public void testPrimitiveBooleanValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertFalse(model.getBooleanPrimitive());
        Assert.assertFalse(model.isBooleanPrimitive());

        model.setBooleanPrimitive(true);

        Assert.assertTrue(model.getBooleanPrimitive());
        Assert.assertTrue(model.isBooleanPrimitive());
    }

    @Test
    public void testDoubleValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getDouble());

        model.setDouble(new Double(1.0D));

        Assert.assertEquals(new Double(1.0D), model.getDouble());
    }

    @Test
    public void testDoublePrimitiveValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(0.0d, model.getDoublePrimitive(), 0.0d);

        model.setDoublePrimitive(1.5d);

        Assert.assertEquals(1.5d, model.getDoublePrimitive(), 0.0d);
    }

    @Test
    public void testIntegerValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getInteger());

        model.setInteger(new Integer(10));

        Assert.assertEquals(new Integer(10), model.getInteger());
    }

    @Test
    public void testIntValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(0, model.getInt());

        model.setInt(1000);

        Assert.assertEquals(1000, model.getInt());
    }

    @Test
    public void testStringValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getString());

        model.setString("foobar");

        Assert.assertEquals("foobar", model.getString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedPrimitiveSetter() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setLong(0L);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedTypeSetter() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setBean(new Bean());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedPrimitiveGetter() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.getLong();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedTypeGetter() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.getBean();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetterVoid() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.getFoo();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetterWithParam() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.getFoo(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetterReturns() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setFoo(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetterNoParam() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setFoo();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetterTwoParams() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setFoo(1, 2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInvalidSetterMethodName() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setfoo(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInvalidGetterMethodName() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.isbar();
    }
}
