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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.HtmlTemplate;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.router.Location;
import com.vaadin.hummingbird.router.Router;
import com.vaadin.hummingbird.router.ViewRendererTest.TestView;
import com.vaadin.hummingbird.template.InlineTemplate;
import com.vaadin.hummingbird.template.TemplateParseException;
import com.vaadin.server.communication.ServerRpcHandlerTest;
import com.vaadin.ui.ComponentTest.TestComponent;

/**
 * @author Vaadin Ltd
 *
 */
public class TemplateTest {

    private static class TestTemplate extends Template {
        TestTemplate() {
            super(new ByteArrayInputStream(
                    "<div>foo</div>".getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static class NullTemplate extends Template {
        NullTemplate() {
            super((String) null);
        }
    }

    public static class TemplateParentView extends InlineTemplate {
        public TemplateParentView() {
            super("<div><h1>Header</h1>@child@</div>");
        }
    }

    @Tag("H1")
    public static class H1TestComponent extends Component {

    }

    public static class TemplateUsingStreamConstructor extends InlineTemplate {

        @Id("header")
        protected H1TestComponent header;

        public TemplateUsingStreamConstructor() {
            super("<div><h1 id='header'>Header</h1>@child@<div id='footer'></div></div>");
        }

    }

    public static class SimpleTemplate extends InlineTemplate {
        public SimpleTemplate() {
            super("<h1 id='root'></h1>");
        }

    }

    public static class TemplateMapToRoot extends SimpleTemplate {

        @Id("root")
        protected H1TestComponent root;

    }

    public static class TemplateMapInvalidFieldType extends SimpleTemplate {

        @Id("root")
        protected String root;

    }

    public static class TemplateNonExistingIdField extends SimpleTemplate {

        @Id("foo")
        protected TestComponent root;

    }

    public static class TemplateEmptyIdField extends SimpleTemplate {
        @Id("")
        protected TestComponent root;

    }

    public static class TemplateWithSameIdMultipleTimes extends InlineTemplate {
        @Id("myid")
        protected TestComponent component;

        public TemplateWithSameIdMultipleTimes() {
            super("<div><span id='myid'></span><b id='myid'></b></div>");
        }

    }

    public static class TemplateWithParentComponentMapping
            extends TemplateUsingStreamConstructor {

        @Id("footer")
        private TestComponent footer;
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

    @Tag("SPAN")
    public static class TestSpan extends Component {

    }

    @HtmlTemplate("../hummingbird/template/main.html")
    private static class TemplateDefaultConstructor extends Template {
        @Id("main")
        private TestSpan span;
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
    public void useTemplateAsParentView() {
        Router router = new Router();
        router.reconfigure(c -> {
            c.setRoute("", TestView.class, TemplateParentView.class);
            c.setRoute("empty", TemplateParentView.class);
        });

        UI ui = new UI();
        router.navigate(ui, new Location(""));

        Assert.assertEquals(
                Arrays.asList(TestView.class, TemplateParentView.class),
                ui.getActiveViewChain().stream().map(Object::getClass)
                        .collect(Collectors.toList()));
        Element uiContent = ui.getElement().getChild(0);

        Assert.assertEquals("div", uiContent.getTag());

        Assert.assertEquals(2, uiContent.getChildCount());
        Assert.assertEquals("h1", uiContent.getChild(0).getTag());
        Assert.assertEquals("div", uiContent.getChild(1).getTag());

        router.navigate(ui, new Location("empty"));

        Assert.assertEquals(Arrays.asList(TemplateParentView.class),
                ui.getActiveViewChain().stream().map(Object::getClass)
                        .collect(Collectors.toList()));

        Assert.assertEquals(1, uiContent.getChildCount());
        Assert.assertEquals("h1", uiContent.getChild(0).getTag());
    }

    @Tag("div")
    public static class ParentComponent extends Component
            implements HasComponents {

    }

    @Test
    public void templateParentComponent() {
        ParentComponent p = new ParentComponent();
        InlineTemplate template = new InlineTemplate("<div>Foo</div>");
        p.add(template);

        Assert.assertEquals(p, template.getParent().get());
    }

    @Test(expected = TemplateParseException.class)
    public void templateInputStreamWithInclude() {
        new InlineTemplate("<div>@include bar.html@</div>");
    }

    @Test
    public void mapComponentsDefaultConstructor() {
        TemplateDefaultConstructor t = new TemplateDefaultConstructor();
        Assert.assertNotNull(t.span);
        Assert.assertEquals("span", t.span.getElement().getTag());
    }

    @Test
    public void mapComponentsStreamConstructor() {
        TemplateUsingStreamConstructor t = new TemplateUsingStreamConstructor();
        Assert.assertNotNull(t.header);
        Assert.assertEquals("h1", t.header.getElement().getTag());
    }

    @Test
    public void mapComponentsParentClass() {
        TemplateWithParentComponentMapping t = new TemplateWithParentComponentMapping();
        Assert.assertNotNull(t.header);
        Assert.assertEquals("h1", t.header.getElement().getTag());
        Assert.assertNotNull(t.footer);
        Assert.assertEquals("div", t.footer.getElement().getTag());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapTemplateRoot() {
        new TemplateMapToRoot();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapInvalidFieldType() {
        new TemplateMapInvalidFieldType();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapNonExistingId() {
        new TemplateNonExistingIdField();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapEmptyId() {
        new TemplateEmptyIdField();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapWithSameIdUsedMultipleTimes() {
        new TemplateWithSameIdMultipleTimes();
    }

    @Before
    @After
    public void checkThreadLocal() {
        Assert.assertNull(Component.elementToMapTo.get());
    }

    @Test
    public void rootElementEventListener() throws Exception {
        UI ui = new UI();
        Template t = new InlineTemplate("<root><child></child></root>");
        Element element = t.getElement();
        ui.add(t);
        AtomicInteger invoked = new AtomicInteger(0);
        element.addEventListener("test-event", e -> {
            invoked.incrementAndGet();
        });
        ServerRpcHandlerTest.sendElementEvent(element, ui, "test-event", null);
        Assert.assertEquals(1, invoked.get());
    }

    @Test
    public void childElementEventListener() throws Exception {
        UI ui = new UI();
        Template t = new InlineTemplate("<root><child></child></root>");
        Element element = t.getElement().getChild(0);
        ui.add(t);
        AtomicInteger invoked = new AtomicInteger(0);
        element.addEventListener("test-event", e -> {
            invoked.incrementAndGet();
        });
        ServerRpcHandlerTest.sendElementEvent(element, ui, "test-event", null);
        Assert.assertEquals(1, invoked.get());
    }

}
