/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.ModelMap;

public class ModelPathResolverTest {

    StateNode root;

    @Before
    public void setup() {
        root = TemplateElementStateProvider.createRootNode();
    }

    @Test
    public void resolveEmptyPath() {
        ModelPathResolver resolver = new ModelPathResolver("");
        ModelMap map = resolver.resolveModelMap(root);
        Assert.assertEquals(root.getFeature(ModelMap.class), map);
    }

    @Test
    public void resolveProperty() {
        ModelPathResolver resolver = new ModelPathResolver("foo");
        ModelMap map = resolver.resolveModelMap(root);
        Assert.assertEquals(root.getFeature(ModelMap.class), map);
    }

    @Test
    public void resolveSubProperty() {
        ModelMap rootMap = root.getFeature(ModelMap.class);
        Assert.assertFalse(rootMap.hasValue("foo"));

        ModelMap map = new ModelPathResolver("foo.bar").resolveModelMap(root);
        Assert.assertTrue(rootMap.hasValue("foo"));
        ModelMap fooMap = ((StateNode) rootMap.getValue("foo"))
                .getFeature(ModelMap.class);
        Assert.assertEquals(fooMap, map);
    }

    @Test
    public void resolveSubSubProperty() {
        ModelMap map = new ModelPathResolver("foo.bar.baz")
                .resolveModelMap(root);
        ModelMap rootMap = root.getFeature(ModelMap.class);
        ModelMap fooMap = ((StateNode) rootMap.getValue("foo"))
                .getFeature(ModelMap.class);
        ModelMap barMap = ((StateNode) fooMap.getValue("bar"))
                .getFeature(ModelMap.class);

        Assert.assertEquals(barMap, map);
    }

    @Test
    public void propertyNameForEmpty() {
        ModelPathResolver resolver = new ModelPathResolver("");
        Assert.assertEquals("", resolver.getPropertyName());
    }

    @Test
    public void propertyNameForProperty() {
        ModelPathResolver resolver = new ModelPathResolver("foo");
        Assert.assertEquals("foo", resolver.getPropertyName());
    }

    @Test
    public void propertyNameForSubProperty() {
        ModelPathResolver resolver = new ModelPathResolver("foo.bar");
        Assert.assertEquals("bar", resolver.getPropertyName());
    }

    @Test
    public void propertyNameForSubSubProperty() {
        ModelPathResolver resolver = new ModelPathResolver("foo.bar.baz");
        Assert.assertEquals("baz", resolver.getPropertyName());
    }

}
