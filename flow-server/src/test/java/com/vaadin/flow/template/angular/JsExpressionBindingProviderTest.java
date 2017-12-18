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
package com.vaadin.flow.template.angular;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.internal.nodefeature.TemplateMap;
import com.vaadin.flow.template.angular.JsExpressionBindingProvider;
import com.vaadin.flow.template.angular.model.BeanModelType;
import com.vaadin.flow.template.angular.model.TestModelDescriptor;
import com.vaadin.flow.templatemodel.BasicModelType;
import com.vaadin.flow.templatemodel.ModelType;

/**
 * @author Vaadin Ltd
 *
 */
public class JsExpressionBindingProviderTest {

    @Test
    public void setTemplateProperty_useJsExpression() {
        JsExpressionBindingProvider binding = new JsExpressionBindingProvider(
                "bar +'foo'");
        StateNode node = new StateNode(ModelMap.class, TemplateMap.class);
        node.getFeature(TemplateMap.class).setModelDescriptor(
                new TestModelDescriptor(Collections.singletonMap("bar",
                        BasicModelType.get(String.class).get())));
        ModelMap.get(node).setValue("bar", "modelValue");

        Assert.assertEquals("modelValuefoo", binding.getValue(node));
    }

    @Test
    public void jsExpressionWithSubProperty() {
        JsExpressionBindingProvider binding = new JsExpressionBindingProvider(
                "bean.property");

        StateNode beanNode = new StateNode(ModelMap.class);
        ModelMap beanModel = ModelMap.get(beanNode);
        beanModel.setValue("property", "foo");

        StateNode rootNode = new StateNode(TemplateMap.class, ModelMap.class);

        Map<String, ModelType> beanProperties = Collections.singletonMap(
                "property", BasicModelType.get(String.class).get());
        Map<String, ModelType> modelProperties = Collections.singletonMap(
                "bean", new BeanModelType<>(Object.class, beanProperties));
        rootNode.getFeature(TemplateMap.class)
                .setModelDescriptor(new TestModelDescriptor(modelProperties));

        ModelMap.get(rootNode).setValue("bean", beanNode);

        Assert.assertEquals("foo", binding.getValue(rootNode));
    }

}
