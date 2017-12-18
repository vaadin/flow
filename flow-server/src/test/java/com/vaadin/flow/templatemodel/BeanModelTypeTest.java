/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.templatemodel;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.template.angular.model.BeanModelType;
import com.vaadin.flow.templatemodel.PropertyFilter;

public class BeanModelTypeTest {
    // Partial overlap with Bean
    public static class DifferentBean {
        private double intValue;
        private String string;
        private Date date;

        public DifferentBean() {
            // Using default values
        }

        public DifferentBean(long value) {
            intValue = value;
            string = String.valueOf(value);
            date = new Date(value);
        }

        public double getIntValue() {
            return intValue;
        }

        public void setIntValue(double intValue) {
            this.intValue = intValue;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }
    }

    @Test
    public void importBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL);

        ModelMap model = ModelMap.get(new StateNode(ModelMap.class));

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);

        assertThreeBean(model);
    }

    private void assertThreeBean(ModelMap model) {
        Assert.assertEquals(7, model.getKeys().count());

        Assert.assertEquals(Integer.valueOf(3), model.getValue("intValue"));
        Assert.assertEquals(Integer.valueOf(3), model.getValue("intObject"));

        Assert.assertEquals(Double.valueOf(3), model.getValue("doubleValue"));
        Assert.assertEquals(Double.valueOf(3), model.getValue("doubleObject"));

        Assert.assertEquals(Boolean.TRUE, model.getValue("booleanValue"));
        Assert.assertEquals(Boolean.TRUE, model.getValue("booleanObject"));

        Assert.assertEquals("3", model.getValue("string"));
    }

    @Test
    public void importBean_withTypeFilter() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                new PropertyFilter(name -> "intValue".equals(name)));

        ModelMap model = ModelMap.get(new StateNode(ModelMap.class));

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);

        Assert.assertEquals(1, model.getKeys().count());

        Assert.assertEquals(Integer.valueOf(3), model.getValue("intValue"));
    }

    @Test
    public void importBean_withImportFilter() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL);

        ModelMap model = ModelMap.get(new StateNode(ModelMap.class));

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean,
                new PropertyFilter(name -> "intObject".equals(name)));

        Assert.assertEquals(1, model.getKeys().count());

        Assert.assertEquals(Integer.valueOf(3), model.getValue("intObject"));
    }

    @Test
    public void importBean_differentBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL);

        ModelMap model = ModelMap.get(new StateNode(ModelMap.class));

        DifferentBean bean = new DifferentBean(3);

        // Ignore intValue which has an incompatible type
        beanType.importProperties(model, bean,
                new PropertyFilter(name -> !"intValue".equals(name)));

        Assert.assertEquals(1, model.getKeys().count());

        Assert.assertEquals("3", model.getValue("string"));

        Assert.assertFalse(model.hasValue("date"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void importBean_incompatibleBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL);

        ModelMap model = ModelMap.get(new StateNode(ModelMap.class));

        DifferentBean bean = new DifferentBean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);
    }

    @Test
    public void modelToApplication() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL);

        ModelMap model = ModelMap.get(new StateNode(ModelMap.class));
        model.setValue("string", "3");
        model.setValue("intValue", Integer.valueOf(3));

        Bean bean = beanType.modelToApplication(model.getNode());

        Assert.assertEquals("3", bean.getString());
        Assert.assertEquals(3, bean.getIntValue());
        Assert.assertNull(bean.getIntObject());
    }

    @Test
    public void applicationToModel() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL);

        Bean bean = new Bean(3);

        StateNode applicationToModel = beanType.applicationToModel(bean,
                PropertyFilter.ACCEPT_ALL);

        ModelMap model = ModelMap.get(applicationToModel);

        assertThreeBean(model);
    }

    @Test
    public void applicationToModel_filtered() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                new PropertyFilter(name -> !name.equals("intValue")));

        Bean bean = new Bean(3);

        StateNode applicationToModel = beanType.applicationToModel(bean,
                new PropertyFilter(name -> name.equals("string")
                        || name.equals("intValue")));

        ModelMap model = ModelMap.get(applicationToModel);

        Assert.assertEquals(Arrays.asList("string"),
                model.getKeys().collect(Collectors.toList()));

        Assert.assertEquals("3", model.getValue("string"));
    }
}
