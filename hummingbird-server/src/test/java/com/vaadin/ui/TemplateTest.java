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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlTemplate;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.nodefeature.TemplateMetadataFeature;
import com.vaadin.hummingbird.template.InlineTemplate;

/**
 * @author Vaadin Ltd
 *
 */
public class TemplateTest {

    private static class TestTemplate extends InlineTemplate {
        TestTemplate() {
            super("<div>foo</div>");
        }
    }

    private static class NullTemplate extends Template {
        NullTemplate() {
            super((String) null);
        }
    }

    @HtmlTemplate("samePackage.html")
    private static class AnnotatedRelativePathTemplate extends Template {

    }

    @HtmlTemplate("no-extension")
    private static class AnnotatedNoExtensionTemplate extends Template {

    }

    @HtmlTemplate("/com/htmlSnippet.html")
    private static class AnnotatedAbsolutePathTemplate extends Template {

    }

    @HtmlTemplate("/root.html")
    private static class AnnotatedRootPathTemplate extends Template {

    }

    private static class InheritedAnnotationTemplate
            extends AnnotatedAbsolutePathTemplate {

    }

    private static class EventHandlerTemplate extends TestTemplate {

        private boolean isInvoked;

        @EventHandler
        protected void operation() {
            isInvoked = true;
        }

        /**
         * won't be introspected by {@link TemplateMetadataFeature}
         */
        @EventHandler
        private void wrongVisibility() {
        }

        /**
         * won't be introspected by {@link TemplateMetadataFeature}
         */
        @EventHandler
        protected void hasParameter(int i) {
        }

        @EventHandler
        protected void throwsUncheckedException() throws NullPointerException {
            String str = null;
            str.length();
        }
    }

    private static class EventHandlerTemplateSubclass
            extends EventHandlerTemplate {

    }

    @Test
    public void inputStreamInConstructor() {
        Template template = new TestTemplate();
        Element element = template.getElement();

        Assert.assertEquals("div", element.getTag());
        Assert.assertEquals("foo", element.getTextContent());
    }

    @Test
    public void templateHasExpectedNamespaces() {
        Template template = new TestTemplate();
        StateNode node = template.getElement().getNode();

        Assert.assertNotNull(node.getFeature(TemplateMap.class));
        Assert.assertNotNull(node.getFeature(ComponentMapping.class));
        Assert.assertNotNull(node.getFeature(ModelMap.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullTemplate() {
        new NullTemplate();
    }

    @Test
    public void htmlAnnotation_htmlFileInSamePackage() {
        AnnotatedRelativePathTemplate template = new AnnotatedRelativePathTemplate();
        Assert.assertEquals("same_package",
                template.getElement().getAttribute("id"));
    }

    @Test
    public void htmlAnnotation_htmlFileInDifferentPackage() {
        AnnotatedAbsolutePathTemplate template = new AnnotatedAbsolutePathTemplate();
        Assert.assertEquals("absolute",
                template.getElement().getAttribute("id"));
    }

    @Test
    public void htmlAnnotation_htmlFileInRootPackage() {
        AnnotatedRootPathTemplate template = new AnnotatedRootPathTemplate();
        Assert.assertEquals("root", template.getElement().getAttribute("id"));
    }

    @Test
    public void htmlAnnotation_inherited() {
        InheritedAnnotationTemplate template = new InheritedAnnotationTemplate();
        Assert.assertEquals("absolute",
                template.getElement().getAttribute("id"));
    }

    @Test
    public void htmlAnnotation_noExtension() {
        AnnotatedNoExtensionTemplate template = new AnnotatedNoExtensionTemplate();
        Assert.assertEquals("no-extension",
                template.getElement().getAttribute("id"));
    }

    @Test
    public void invokeEventHandlerMethod_methodInsideClass() {
        EventHandlerTemplate template = new EventHandlerTemplate();
        template.invokeEventHandlerMethod("operation");

        Assert.assertTrue(template.isInvoked);
    }

    @Test(expected = AssertionError.class)
    public void invokeEventHandlerMethod_badVisibility() {
        EventHandlerTemplate template = new EventHandlerTemplate();
        template.invokeEventHandlerMethod("wrongVisibility");
    }

    @Test(expected = AssertionError.class)
    public void invokeEventHandlerMethod_hasParameter() {
        EventHandlerTemplate template = new EventHandlerTemplate();
        template.invokeEventHandlerMethod("hasParameter");
    }

    @Test
    public void invokeEventHandlerMethod_throwsException() {
        EventHandlerTemplate template = new EventHandlerTemplate();
        try {
            template.invokeEventHandlerMethod("throwsUncheckedException");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof NullPointerException);
        }

    }

    @Test
    public void invokeEventHandlerMethod_methodInsideSuperClass() {
        EventHandlerTemplateSubclass template = new EventHandlerTemplateSubclass();
        template.invokeEventHandlerMethod("operation");

        Assert.assertTrue(((EventHandlerTemplate) template).isInvoked);
    }
}
