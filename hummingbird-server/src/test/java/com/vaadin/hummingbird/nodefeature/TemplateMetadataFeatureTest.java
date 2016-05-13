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
package com.vaadin.hummingbird.nodefeature;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateTree;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

/**
 * @author Vaadin Ltd
 *
 */
public class TemplateMetadataFeatureTest {

    static class Template1 extends Template {
        public Template1() {
            super(new ByteArrayInputStream(
                    "<div></div>".getBytes(StandardCharsets.UTF_8)));
        }

        @EventHandler
        protected void method() {

        }
    }

    static class Template2 extends Template1 {

        @EventHandler
        protected void method(String arg) {

        }
    }

    static class Template3 extends Template1 {

        @EventHandler
        protected int op() {
            return 0;
        }
    }

    static class Template4 extends Template1 {

        @EventHandler
        protected void op() {
        }

        @EventHandler
        public void handle() {
        }
    }

    static class Template5 extends Template1 {

        @Override
        @EventHandler
        protected void method() {

        }
    }

    static class Template6 extends Template2 {

        @Override
        @EventHandler
        protected void method() {

        }
    }

    @Test
    public void noValuesBeforeAttach() {
        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template1());

        TemplateMetadataFeature feature = stateNode
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(0, feature.size());
    }

    @Test
    public void attach_metatdataContainsTemplateMethod() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template1());

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);

        TemplateMetadataFeature feature = stateNode
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(1, feature.size());
        Assert.assertEquals(Template1.class.getDeclaredMethods()[0].getName(),
                feature.get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodHasArg_ExceptionIsThrown() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template2());

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodReturnType_ExceptionIsThrown() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template3());

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
    }

    @Test
    public void attach_metatdataContainsAllTemplateMethods() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template4());

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);

        TemplateMetadataFeature feature = stateNode
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(3, feature.size());

        HashSet<String> methods = Stream
                .of(Template4.class.getDeclaredMethods()).map(Method::getName)
                .collect(Collectors.toCollection(HashSet::new));
        methods.add(Template4.class.getDeclaredMethods()[0].getName());

        for (int i = 0; i < feature.size(); i++) {
            methods.remove(feature.get(i));
        }
        Assert.assertTrue(methods.isEmpty());
    }

    @Test
    public void attach_metatdataContainsOnlyOneTemplateMethod() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template5());

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);

        TemplateMetadataFeature feature = stateNode
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(1, feature.size());
        Assert.assertEquals(Template5.class.getDeclaredMethods()[0].getName(),
                feature.get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodReturnTypeInSuperClass_ExceptionIsThrown() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template6());

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
    }

    @Test(expected = AssertionError.class)
    public void attach_noFeature() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(TemplateMetadataFeature.class);

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
    }

    @Test(expected = AssertionError.class)
    public void attach_noComponent() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                TemplateMetadataFeature.class);

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
    }
}
