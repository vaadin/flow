package com.vaadin.hummingbird.template.model;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.template.model.TemplateModelTest.NoModelTemplate;
import com.vaadin.ui.Template;

public class TemplateModelBeanUtilTest {

    public static class BeanWithInvalidProperty {
        private StringBuilder sb;

        public StringBuilder getSb() {
            return sb;
        }

        public void setSb(StringBuilder sb) {
            this.sb = sb;
        }
    }

    public static class BeanTemplate extends NoModelTemplate {

        @Override
        public BeanModel getModel() {
            return (BeanModel) super.getModel();
        }
    }

    @Test
    public void testBeanToModelMapImport() {
        Bean bean = new Bean();
        bean.setBooleanObject(Boolean.TRUE);
        bean.setBooleanValue(true);
        bean.setIntValue(1);
        bean.setIntObject(Integer.valueOf(2));
        bean.setDoubleValue(1.0d);
        bean.setDoubleObject(Double.valueOf(2.0d));
        bean.setString("foobar");

        NoModelTemplate template = new NoModelTemplate();
        template.getModel().importBean(bean);

        verifyBeanToModelMap(bean, template);
    }

    @Test
    public void testBeanToModelImport() {
        Bean bean = new Bean();
        bean.setBooleanObject(Boolean.TRUE);
        bean.setBooleanValue(true);
        bean.setIntValue(1);
        bean.setIntObject(Integer.valueOf(2));
        bean.setDoubleValue(1.0d);
        bean.setDoubleObject(Double.valueOf(2.0d));
        bean.setString("foobar");

        BeanTemplate beanTemplate = new BeanTemplate();
        BeanModel beanModel = beanTemplate.getModel();
        beanTemplate.getModel().importBean(bean);

        verifyBeanToModelViaInterface(bean, beanModel);
        verifyBeanToModelMap(bean, beanTemplate);

        Bean bean2 = new Bean();
        bean.setBooleanObject(Boolean.FALSE);
        bean.setBooleanValue(false);
        bean.setIntValue(5);
        bean.setIntObject(Integer.valueOf(123));
        bean.setDoubleValue(10.0d);
        bean.setDoubleObject(Double.valueOf(20.0d));
        bean.setString("shazbot");

        beanTemplate.getModel().importBean(bean);

        verifyBeanToModelViaInterface(bean2, beanModel);
        verifyBeanToModelMap(bean2, beanTemplate);
    }

    @Test
    public void testBeanToModelImportNull() {
        Bean bean = new Bean();
        BeanTemplate beanTemplate = new BeanTemplate();
        BeanModel beanModel = beanTemplate.getModel();
        beanTemplate.getModel().importBean(bean);

        verifyBeanToModelViaInterface(bean, beanModel);
        verifyBeanToModelMap(bean, beanTemplate);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInvalidBeanProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithInvalidProperty bean = new BeanWithInvalidProperty();
        // won't crash if both are null
        bean.setSb(new StringBuilder());

        template.getModel().importBean(bean);
    }

    private void verifyBeanToModelViaInterface(Bean bean, BeanModel model) {
        Assert.assertEquals(bean.getBooleanObject(), model.getBooleanObject());
        Assert.assertEquals(bean.getDoubleObject(), model.getDoubleObject());
        Assert.assertEquals(bean.getIntObject(), model.getIntObject());
        Assert.assertEquals(bean.isBooleanValue(), model.isBooleanValue());
        Assert.assertEquals(bean.getDoubleValue(), model.getDoubleValue(),
                0.0D);
        Assert.assertEquals(bean.getIntValue(), model.getIntValue());
        Assert.assertEquals(bean.getString(), model.getString());
    }

    private void verifyBeanToModelMap(Bean bean, Template template) {
        ModelMap model = template.getElement().getNode()
                .getFeature(ModelMap.class);
        Assert.assertEquals(bean.getBooleanObject(),
                model.getValue("booleanObject"));
        Assert.assertEquals(bean.getDoubleObject(),
                model.getValue("doubleObject"));
        Assert.assertEquals(bean.getIntObject(), model.getValue("intObject"));
        Assert.assertEquals(bean.isBooleanValue(),
                model.getValue("booleanValue"));
        Assert.assertEquals(bean.getDoubleValue(),
                model.getValue("doubleValue"));
        Assert.assertEquals(bean.getIntValue(), model.getValue("intValue"));
        Assert.assertEquals(bean.getString(), model.getValue("string"));
    }
}
