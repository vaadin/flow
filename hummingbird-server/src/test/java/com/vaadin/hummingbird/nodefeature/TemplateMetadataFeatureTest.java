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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateTree;
import com.vaadin.hummingbird.template.InlineTemplate;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

/**
 * @author Vaadin Ltd
 *
 */
public class TemplateMetadataFeatureTest {

    static class Template1 extends InlineTemplate {
        public Template1() {
            super("<div></div>");
        }

        @EventHandler
        protected void method() {

        }
    }

    static class TemplateWithMethodParameters extends Template1 {

        @EventHandler
        protected void method(String arg) {

        }
    }

    static class TemplateWithMethodReturnValue extends Template1 {

        @EventHandler
        protected int op() {
            return 0;
        }
    }

    static class TemplateWithMethodDeclaringCheckedException extends Template1 {

        @EventHandler
        protected void op() throws IOException {

        }
    }

    static class TemplateWithMethodDeclaringUncheckedException
            extends Template1 {

        @EventHandler
        protected void op() throws NullPointerException {

        }
    }

    static class ChildTemplateWithMultipleMethods extends Template1 {

        @EventHandler
        protected void op() {
        }

        @EventHandler
        public void handle() {
        }
    }

    static class ChildTemplateOverridingMethod extends Template1 {

        @Override
        @EventHandler
        protected void method() {

        }
    }

    static class ChildTemplateOfIncorrectTemplate
            extends TemplateWithMethodParameters {

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
    public void attach_metadataContainsTemplateMethod() {
        UI ui = new UI();

        Template template = new Template1();
        ui.add(template);

        TemplateMetadataFeature feature = template.getElement().getNode()
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(1, feature.size());
        Assert.assertEquals(
                getDeclaredMethods(Template1.class).findFirst().get(),
                feature.get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodHasArg_ExceptionIsThrown() {
        UI ui = new UI();

        Template template = new TemplateWithMethodParameters();
        ui.add(template);
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodReturnType_ExceptionIsThrown() {
        UI ui = new UI();

        Template template = new TemplateWithMethodReturnValue();
        ui.add(template);
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodCheckedExcepotion_ExceptionIsThrown() {
        UI ui = new UI();

        Template template = new TemplateWithMethodDeclaringCheckedException();
        ui.add(template);
    }

    @Test
    public void attach_methodWithUncheckedException_metadataContainsTemplateMethod() {
        UI ui = new UI();

        Template template = new TemplateWithMethodDeclaringUncheckedException();
        ui.add(template);

        TemplateMetadataFeature feature = template.getElement().getNode()
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(2, feature.size());
        Assert.assertEquals(getDeclaredMethods(
                TemplateWithMethodDeclaringUncheckedException.class).findFirst()
                        .get(),
                feature.get(0));
    }

    @Test
    public void attach_metadataContainsAllTemplateMethods() {
        UI ui = new UI();

        Template template = new ChildTemplateWithMultipleMethods();
        ui.add(template);

        TemplateMetadataFeature feature = template.getElement().getNode()
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(3, feature.size());

        HashSet<String> methods = getDeclaredMethods(
                ChildTemplateWithMultipleMethods.class)
                        .collect(Collectors.toCollection(HashSet::new));
        methods.add(getDeclaredMethods(Template1.class).findFirst().get());

        for (int i = 0; i < feature.size(); i++) {
            methods.remove(feature.get(i));
        }
        Assert.assertTrue(
                "Feature doesn't contain methods: " + methods.toString(),
                methods.isEmpty());
    }

    @Test
    public void attach_metadataContainsOnlyOneTemplateMethod() {
        UI ui = new UI();

        Template template = new ChildTemplateOverridingMethod();
        ui.add(template);

        TemplateMetadataFeature feature = template.getElement().getNode()
                .getFeature(TemplateMetadataFeature.class);
        Assert.assertEquals(1, feature.size());
        Assert.assertEquals(
                getDeclaredMethods(ChildTemplateOverridingMethod.class)
                        .findFirst().get(),
                feature.get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodReturnTypeInSuperClass_ExceptionIsThrown() {
        UI ui = new UI();

        Template template = new ChildTemplateOfIncorrectTemplate();
        ui.add(template);
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

    private Stream<String> getDeclaredMethods(Class<?> clazz) {
        // Code coverage jacoco adds nice unexpected private static method
        // $jacocoInit which nobody needs
        return Stream.of(clazz.getDeclaredMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers())
                        && !Modifier.isPrivate(method.getModifiers()))
                .map(Method::getName);
    }
}
