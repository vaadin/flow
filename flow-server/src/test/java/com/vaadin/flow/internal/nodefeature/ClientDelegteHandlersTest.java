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
package com.vaadin.flow.internal.nodefeature;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.ClientDelegate;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ClientDelegateHandlers;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.SerializableNodeList;
import com.vaadin.flow.template.angular.AngularTemplate;
import com.vaadin.flow.template.angular.InlineTemplate;

import elemental.json.JsonValue;

public class ClientDelegteHandlersTest {

    static class Template1 extends InlineTemplate {
        public Template1() {
            super("<div></div>");
        }

        @ClientDelegate
        protected void method() {

        }
    }

    static class TemplateWithGoodParametersMethods extends Template1 {

        @ClientDelegate
        protected void method1(String arg) {

        }

        @ClientDelegate
        protected void method2(Integer arg) {

        }

        @ClientDelegate
        protected void method3(Boolean arg) {

        }

        @ClientDelegate
        protected void method4(JsonValue arg) {

        }

        @ClientDelegate
        protected void method5(Integer[] arg) {

        }

        @ClientDelegate
        protected void method6(String... arg) {

        }

        @ClientDelegate
        protected void method7(int arg) {

        }

        @ClientDelegate
        protected void method8(double arg) {

        }

        @ClientDelegate
        protected void method9(boolean arg) {

        }
    }

    static class TemplateWithMethodReturnValue extends Template1 {

        @ClientDelegate
        protected int op() {
            return 0;
        }
    }

    static class TemplateWithMethodDeclaringCheckedException extends Template1 {

        @ClientDelegate
        protected void op() throws IOException {

        }
    }

    static class TemplateWithMethodDeclaringUncheckedException
            extends Template1 {

        @ClientDelegate
        protected void op() throws NullPointerException {

        }
    }

    static class ChildTemplateWithMultipleMethods extends Template1 {

        @ClientDelegate
        protected void op() {
        }

        @ClientDelegate
        public void handle() {
        }
    }

    static class ChildTemplateWithOverriddenMethod extends Template1 {

        @Override
        @ClientDelegate
        protected void method() {

        }
    }

    static class ChildTemplateWithOverloadedMethod extends Template1 {

        @ClientDelegate
        protected void method(int i) {

        }
    }

    @Tag("div")
    static class NonTemplateComponentWithoutEventHandler extends Component {
    }

    @Tag("div")
    static class NonTemplateComponentWithEventHandler extends Component {

        @ClientDelegate
        public void publishedMethod1() {

        }
    }

    @Test
    public void eventHandlersUnrelatedToAttach() {
        StateNode stateNode = new StateNode(ComponentMapping.class,
                ClientDelegateHandlers.class);
        stateNode.getFeature(ComponentMapping.class)
                .setComponent(new Template1());

        ClientDelegateHandlers feature = stateNode
                .getFeature(ClientDelegateHandlers.class);
        Assert.assertEquals(1, feature.size());
    }

    @Test
    public void attach_handlersContainsTemplateMethod() {
        UI ui = new UI();

        AngularTemplate template = new Template1();
        ui.add(template);

        ClientDelegateHandlers feature = template.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        Assert.assertEquals(1, feature.size());
        Assert.assertEquals(
                getDeclaredMethods(Template1.class).findFirst().get(),
                feature.get(0));
    }

    @Test
    public void attach_twoMethodsWithTheSameName_ExceptionIsThrown() {
        UI ui = new UI();

        AngularTemplate template = new ChildTemplateWithOverriddenMethod();
        ui.add(template);
    }

    @Test
    public void attach_methodHasGoodArg_ExceptionIsThrown() {
        UI ui = new UI();

        AngularTemplate template = new TemplateWithGoodParametersMethods();
        ui.add(template);

        ClientDelegateHandlers feature = template.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        Assert.assertEquals(10, feature.size());

        Set<String> methods = getDeclaredMethods(
                TemplateWithGoodParametersMethods.class)
                        .collect(Collectors.toCollection(HashSet::new));
        methods.add(getDeclaredMethods(Template1.class).findFirst().get());

        for (int i = 0; i < feature.size(); i++) {
            methods.remove(feature.get(i));
        }
        Assert.assertTrue(
                "Feature doesn't contain methods: " + methods.toString(),
                methods.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodReturnType_ExceptionIsThrown() {
        UI ui = new UI();

        AngularTemplate template = new TemplateWithMethodReturnValue();
        ui.add(template);
    }

    @Test(expected = IllegalStateException.class)
    public void attach_methodCheckedExcepotion_ExceptionIsThrown() {
        UI ui = new UI();

        AngularTemplate template = new TemplateWithMethodDeclaringCheckedException();
        ui.add(template);
    }

    @Test
    public void attach_methodWithUncheckedException_handlersContainsTemplateMethod() {
        UI ui = new UI();

        AngularTemplate template = new TemplateWithMethodDeclaringUncheckedException();
        ui.add(template);

        ClientDelegateHandlers feature = template.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        Assert.assertEquals(2, feature.size());
        Assert.assertEquals(getDeclaredMethods(
                TemplateWithMethodDeclaringUncheckedException.class).findFirst()
                        .get(),
                feature.get(0));
    }

    @Test
    public void attach_handlersContainsAllTemplateMethods() {
        UI ui = new UI();

        AngularTemplate template = new ChildTemplateWithMultipleMethods();
        ui.add(template);

        ClientDelegateHandlers feature = template.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        Assert.assertEquals(3, feature.size());

        Set<String> methods = getDeclaredMethods(
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
    public void attach_handlersContainsOnlyOneTemplateMethod() {
        UI ui = new UI();

        AngularTemplate template = new ChildTemplateWithOverriddenMethod();
        ui.add(template);

        ClientDelegateHandlers feature = template.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        Assert.assertEquals(1, feature.size());
        Assert.assertEquals(
                getDeclaredMethods(ChildTemplateWithOverriddenMethod.class)
                        .findFirst().get(),
                feature.get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void attach_overloadedMethod_ExceptionIsThrown() {
        UI ui = new UI();

        AngularTemplate template = new ChildTemplateWithOverloadedMethod();
        ui.add(template);
    }

    @Test
    public void attach_noFeature() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ClientDelegateHandlers.class);

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
        Assert.assertEquals(0,
                stateNode.getFeature(ClientDelegateHandlers.class).size());
    }

    @Test
    public void attach_noComponent() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                ClientDelegateHandlers.class);

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
        Assert.assertEquals(0,
                stateNode.getFeature(ClientDelegateHandlers.class).size());
    }

    @Test
    public void nonTemplateComponentWithEventHandler() {
        UI ui = new UI();
        NonTemplateComponentWithEventHandler component = new NonTemplateComponentWithEventHandler();
        ui.add(component);

        ClientDelegateHandlers feature = component.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        assertListFeature(feature, "publishedMethod1");
    }

    @Test
    public void nonTemplateComponentWithoutEventHandler() {
        UI ui = new UI();
        NonTemplateComponentWithoutEventHandler component = new NonTemplateComponentWithoutEventHandler();
        ui.add(component);

        ClientDelegateHandlers feature = component.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        assertListFeature(feature);
    }

    private void assertListFeature(SerializableNodeList<String> feature,
            String... expected) {
        Assert.assertEquals(expected.length, feature.size());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], feature.get(i));
        }

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
