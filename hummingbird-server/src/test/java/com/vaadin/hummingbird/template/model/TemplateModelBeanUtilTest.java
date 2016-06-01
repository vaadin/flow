package com.vaadin.hummingbird.template.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.template.model.TemplateModelTest.NoModelTemplate;
import com.vaadin.ui.Template;

public class TemplateModelBeanUtilTest {

    public static class BeanWithUnsupportedProperty {
        // not a bean
        private StringBuilder sb;

        public StringBuilder getSb() {
            return sb;
        }

        public void setSb(StringBuilder sb) {
            this.sb = sb;
        }
    }

    public static class BeanWithList {
        private List<Bean> beans;

        public List<Bean> getBeans() {
            return beans;
        }

        public void setBeans(List<Bean> beans) {
            this.beans = beans;
        }
    }

    public static class BeanWithNestedBean {
        private Bean bean;

        public Bean getBean() {
            return bean;
        }

        public void setBean(Bean bean) {
            this.bean = bean;
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
        bean2.setBooleanObject(Boolean.FALSE);
        bean2.setBooleanValue(false);
        bean2.setIntValue(5);
        bean2.setIntObject(Integer.valueOf(123));
        bean2.setDoubleValue(10.0d);
        bean2.setDoubleObject(Double.valueOf(20.0d));
        bean2.setString("shazbot");

        beanTemplate.getModel().importBean(bean2);

        verifyBeanToModelViaInterface(bean2, beanModel);
        verifyBeanToModelMap(bean2, beanTemplate);
    }

    @Test
    public void testBeanToModelImportNullAndDefaultValues() {
        Bean bean = new Bean();
        BeanTemplate beanTemplate = new BeanTemplate();
        BeanModel beanModel = beanTemplate.getModel();
        beanTemplate.getModel().importBean(bean);

        verifyBeanToModelViaInterface(bean, beanModel);
        verifyBeanToModelMap(bean, beanTemplate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBeanProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithUnsupportedProperty bean = new BeanWithUnsupportedProperty();
        // won't crash if both are null
        bean.setSb(new StringBuilder());

        template.getModel().importBean(bean);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBeanWithListProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithList bean = new BeanWithList();
        // won't crash if both are null
        bean.setBeans(new ArrayList<>());

        template.getModel().importBean(bean);
    }

    @Test
    public void testBeanAndModelNotConnected() {
        Bean bean = new Bean();
        bean.setIntValue(1);

        BeanTemplate template = new BeanTemplate();
        BeanModel model = template.getModel();
        model.importBean(bean);

        verifyBeanToModelViaInterface(bean, model);
        verifyBeanToModelMap(bean, template);

        bean.setIntValue(5);
        bean.setString("foobar");

        Assert.assertNotEquals(bean.getIntValue(), model.getIntValue());
        Assert.assertNotEquals(bean.getString(), model.getString());

        model.setString("shazbot");
        model.setIntValue(0);

        Assert.assertNotEquals(bean.getIntValue(), model.getIntValue());
        Assert.assertNotEquals(bean.getString(), model.getString());

        model.importBean(bean);

        verifyBeanToModelViaInterface(bean, model);
        verifyBeanToModelMap(bean, template);
    }

    @Test
    public void testNestedBean() {
        BeanWithNestedBean beanWithNestedBean = new BeanWithNestedBean();
        beanWithNestedBean.setBean(new Bean());

        BeanTemplate template = new BeanTemplate();
        BeanModel model = template.getModel();
        model.importBean(beanWithNestedBean);

        verifyBeanToModelMap(beanWithNestedBean.getBean(),
                ((StateNode) template.getElement().getNode()
                        .getFeature(ModelMap.class).getValue("bean"))
                                .getFeature(ModelMap.class));

        Bean bean2 = new Bean();
        bean2.setBooleanObject(Boolean.FALSE);
        bean2.setBooleanValue(false);
        bean2.setIntValue(5);
        bean2.setIntObject(Integer.valueOf(123));
        bean2.setDoubleValue(10.0d);
        bean2.setDoubleObject(Double.valueOf(20.0d));
        bean2.setString("shazbot");

        beanWithNestedBean.setBean(bean2);

        model.importBean(beanWithNestedBean);

        verifyBeanToModelMap(beanWithNestedBean.getBean(),
                ((StateNode) template.getElement().getNode()
                        .getFeature(ModelMap.class).getValue("bean"))
                                .getFeature(ModelMap.class));
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
        verifyBeanToModelMap(bean, model);
    }

    private void verifyBeanToModelMap(Bean bean, ModelMap model) {
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
