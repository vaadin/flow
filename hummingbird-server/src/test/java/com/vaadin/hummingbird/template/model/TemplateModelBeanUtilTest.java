package com.vaadin.hummingbird.template.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelList;
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

    public static class BeanWithNestedList {
        private List<List<Bean>> beans;

        public List<List<Bean>> getBeans() {
            return beans;
        }

        public void setBeans(List<List<Bean>> beans) {
            this.beans = beans;
        }
    }

    public static class BeanWithPrimitiveList {
        private List<String> strings;

        public List<String> getStrings() {
            return strings;
        }

        public void setStrings(List<String> strings) {
            this.strings = strings;
        }
    }

    public static class BeanWithSet {
        private Set<Bean> beans;

        public Set<Bean> getBeans() {
            return beans;
        }

        public void setBeans(Set<Bean> beans) {
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
    public void testBeanWithSetProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithSet bean = new BeanWithSet();
        // won't crash if both are null
        bean.setBeans(new HashSet<>());

        template.getModel().importBean(bean);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBeanWithNestedListProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithNestedList bean = new BeanWithNestedList();
        // won't crash if both are null
        bean.setBeans(new ArrayList<>());

        template.getModel().importBean(bean);
    }

    @Test
    public void testBeanWithListProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithList bean = new BeanWithList();

        Bean listItem1 = new Bean();
        listItem1.setString("item1");
        Bean listItem2 = new Bean();
        listItem2.setString("item2");

        bean.setBeans(Arrays.asList(listItem1, listItem2));

        template.getModel().importBean(bean);

        ModelMap modelMap = template.getElement().getNode()
                .getFeature(ModelMap.class);
        StateNode beansNode = (StateNode) modelMap.getValue("beans");

        Assert.assertTrue(beansNode.hasFeature(ModelList.class));

        ModelList modelList = beansNode.getFeature(ModelList.class);

        Assert.assertEquals(2, modelList.size());

        verifyBeanToModelMap(listItem1,
                modelList.get(0).getFeature(ModelMap.class));
        verifyBeanToModelMap(listItem2,
                modelList.get(1).getFeature(ModelMap.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBeanWithPrimitiveList() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithPrimitiveList bean = new BeanWithPrimitiveList();
        // won't crash if both are null
        bean.setStrings(new ArrayList<>());

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

        NoModelTemplate template = new NoModelTemplate();
        TemplateModel model = template.getModel();
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

    @Test
    public void testBeanPropertyFilter() {
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

        ArrayList<String> properties = new ArrayList<>();

        // filter out everything except the "string" & intValue property
        Predicate<String> capturingFilter = propertyName -> {
            properties.add(propertyName);
            return propertyName.equals("string")
                    || propertyName.equals("intValue");

        };
        beanModel.importBean(bean, capturingFilter);

        Assert.assertEquals(bean.getString(), beanModel.getString());
        Assert.assertEquals(bean.getIntValue(), beanModel.getIntValue());

        ModelMap modelMap = beanTemplate.getElement().getNode()
                .getFeature(ModelMap.class);

        String[] expectedProperties = new String[] { "string", "intValue",
                "intObject", "doubleObject", "doubleValue", "booleanValue",
                "booleanObject" };
        for (String s : expectedProperties) {
            Assert.assertTrue(properties.contains(s));
        }

        Assert.assertFalse(modelMap.hasValue("booleanObject"));
        Assert.assertFalse(modelMap.hasValue("booleanValue"));
        Assert.assertFalse(modelMap.hasValue("intObject"));
        Assert.assertFalse(modelMap.hasValue("doubleObject"));
        Assert.assertFalse(modelMap.hasValue("doubleValue"));
    }

    @Test
    public void testNestedBeanPropertyFilter() {
        Bean bean = new Bean();
        bean.setBooleanObject(Boolean.TRUE);
        bean.setBooleanValue(true);
        bean.setIntValue(1);
        bean.setIntObject(Integer.valueOf(2));
        bean.setDoubleValue(1.0d);
        bean.setDoubleObject(Double.valueOf(2.0d));
        bean.setString("foobar");

        BeanWithNestedBean beanWithNestedBean = new BeanWithNestedBean();
        beanWithNestedBean.setBean(bean);

        ArrayList<String> properties = new ArrayList<>();
        Predicate<String> capturingFilter = propertyName -> {
            properties.add(propertyName);
            return propertyName.equals("bean")
                    || propertyName.equals("bean.string");
        };

        NoModelTemplate template = new NoModelTemplate();
        template.getModel().importBean(beanWithNestedBean, capturingFilter);

        String[] expectedProperties = new String[] { "bean", "bean.string",
                "bean.intValue", "bean.intObject", "bean.doubleObject",
                "bean.doubleValue", "bean.booleanValue", "bean.booleanObject" };
        for (String s : expectedProperties) {
            Assert.assertTrue("Could not find property " + s,
                    properties.contains(s));
        }

        ModelMap modelMap = template.getElement().getNode()
                .getFeature(ModelMap.class);

        StateNode nestedBeanNode = (StateNode) modelMap.getValue("bean");
        Assert.assertNotNull(nestedBeanNode);

        ModelMap nestedBeanModelMap = nestedBeanNode.getFeature(ModelMap.class);
        Assert.assertEquals(bean.getString(),
                nestedBeanModelMap.getValue("string"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("booleanObject"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("booleanValue"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("intObject"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("doubleObject"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("doubleValue"));
    }

    @Test
    public void importBean_getterThrowsExcpetion_noModelStateNodeCreatedAndImportFails() {
        Bean bean = new Bean() {

            @Override
            public String getString() {
                throw new NullPointerException();
            }
        };
        BeanWithNestedBean beanWithNestedBean = new BeanWithNestedBean();
        beanWithNestedBean.setBean(bean);

        NoModelTemplate template = new NoModelTemplate();
        TemplateModel model = template.getModel();

        ModelMap map = template.getElement().getNode()
                .getFeature(ModelMap.class);

        Assert.assertFalse(map.hasValue("bean"));

        try {
            model.importBean(beanWithNestedBean);
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertFalse(map.hasValue("bean"));
        }
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
