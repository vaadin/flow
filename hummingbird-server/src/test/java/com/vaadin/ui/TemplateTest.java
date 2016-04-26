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
package com.vaadin.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateMap;

/**
 * @author Vaadin Ltd
 *
 */
public class TemplateTest {

    private static class TestTemplate extends Template {
        TestTemplate() {
            super(new ByteArrayInputStream(
                    "<div></div>".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    public void templateHasExpectedNamespaces() {
        Template template = new TestTemplate();
        StateNode node = template.getElement().getNode();

        Assert.assertNotNull(node.getFeature(TemplateMap.class));
        Assert.assertNotNull(node.getFeature(ComponentMapping.class));
        Assert.assertNotNull(node.getFeature(ModelMap.class));
    }

}
