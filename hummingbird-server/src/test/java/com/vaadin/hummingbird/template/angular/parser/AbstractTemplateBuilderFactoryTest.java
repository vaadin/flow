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
package com.vaadin.hummingbird.template.angular.parser;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.template.angular.AbstractBindingValueProvider;
import com.vaadin.hummingbird.template.angular.JsExpressionBindingProvider;
import com.vaadin.hummingbird.template.angular.ModelValueBindingProvider;

/**
 * @author Vaadin Ltd
 *
 */
public class AbstractTemplateBuilderFactoryTest {

    @Test
    public void createExpressionBinding_simpleExpression() {
        AbstractBindingValueProvider binding = AbstractTemplateBuilderFactory
                .createExpressionBinding("bean234");
        Assert.assertEquals(ModelValueBindingProvider.class,
                binding.getClass());

        binding = AbstractTemplateBuilderFactory
                .createExpressionBinding("bean234.property");
        Assert.assertEquals(ModelValueBindingProvider.class,
                binding.getClass());
    }

    @Test
    public void createExpressionBinding_JsExpression() {
        AbstractBindingValueProvider binding = AbstractTemplateBuilderFactory
                .createExpressionBinding("bean234+'name'");
        Assert.assertEquals(JsExpressionBindingProvider.class,
                binding.getClass());
    }
}
