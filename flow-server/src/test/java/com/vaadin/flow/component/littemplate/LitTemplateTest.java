/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.littemplate;

import org.jsoup.Jsoup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

public class LitTemplateTest {

    private VaadinService service = Mockito.mock(VaadinService.class);

    @Tag("foo-bar")
    private static class TestLitTemplate extends LitTemplate {

        @Id("labelId")
        private com.vaadin.flow.dom.Element label;

        public TestLitTemplate(VaadinService service) {
            this((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<foo-bar id='" + tag
                            + "'><label id='labelId' someattribute .property-binding='foo' "
                            + "?attribute-binding='bar' another-binding='${bar}' "
                            + "another-attribute='baz' hidden></foo-bar>")),
                    service);
        }

        TestLitTemplate(LitTemplateParser parser, VaadinService service) {
            super(parser, service);
        }

    }

    @Tag("foo-bar")
    private static class DisabledElementTemplate extends LitTemplate {

        @Id("labelId")
        private com.vaadin.flow.dom.Element label;

        public DisabledElementTemplate(VaadinService service) {
            this((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<foo-bar id='" + tag
                            + "'><label id='labelId' disabled></foo-bar>")),
                    service);
        }

        DisabledElementTemplate(LitTemplateParser parser,
                VaadinService service) {
            super(parser, service);
        }
    }

    @Tag("foo-bar")
    private static class ElementWithTextLitTemplate extends LitTemplate {

        @Id("labelId")
        private com.vaadin.flow.dom.Element label;

        @Id("hasHierarchy")
        private com.vaadin.flow.dom.Element div;

        public ElementWithTextLitTemplate(VaadinService service) {
            this((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<dom-module id='" + tag
                            + "'><label id='labelId'>foo bar</label>"
                            + "<div id='hasHierarchy'>baz <a>foo</a> bar</div></dom-module>")),
                    service);
        }

        ElementWithTextLitTemplate(LitTemplateParser parser,
                VaadinService service) {
            super(parser, service);
        }

    }

    @Tag("inject-component-which-set-property")
    public static class InjectComponentWhichSetProperty extends LitTemplate {

        @Id("child")
        private ComponentSetProperty child;

        public InjectComponentWhichSetProperty(VaadinService service) {
            super((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<inject-component-which-set-property id='"
                            + tag
                            + "'><template><component-set-property id='child' "
                            + "foo='bar'></component-set-property></template>"
                            + "</inject-component-which-set-property>")),
                    service);
        }
    }

    @Tag("component-set-property")
    public static class ComponentSetProperty extends LitTemplate {
        public ComponentSetProperty(VaadinService service) {
            super((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<component-set-property id='" + tag
                            + "'></component-set-property>")),
                    service);
            getElement().setProperty("foo", "baz");
        }
    }

    @Before
    public void setUp() {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
    }

    @Test
    public void attachExistingElementWithAttributeValue_elementHasAttribute() {
        TestLitTemplate template = new TestLitTemplate(service);

        Assert.assertTrue(template.label.hasAttribute("id"));
        Assert.assertEquals("labelId", template.label.getAttribute("id"));

        Assert.assertTrue(template.label.hasProperty("someattribute"));
        Assert.assertNotNull(template.label.getProperty("someattribute"));
        Assert.assertEquals(Boolean.TRUE.toString(),
                template.label.getProperty("someattribute"));

        Assert.assertFalse(template.label.hasProperty("property-binding"));
        Assert.assertFalse(template.label.hasProperty("propertyBinding"));

        Assert.assertFalse(template.label.hasProperty("another-binding"));
        Assert.assertFalse(template.label.hasProperty("anotherBinding"));

        Assert.assertFalse(template.label.hasProperty("attribute-binding"));
        Assert.assertFalse(template.label.hasProperty("attributeBinding"));
        Assert.assertFalse(template.label.hasProperty("attribute-binding$"));
        Assert.assertFalse(template.label.hasProperty("attributeBinding$"));

        Assert.assertTrue(template.label.hasProperty("another-attribute"));
        Assert.assertEquals("baz",
                template.label.getProperty("another-attribute"));

        Assert.assertTrue(template.label.hasAttribute("hidden"));
        Assert.assertEquals(Boolean.TRUE.toString(),
                template.label.getAttribute("hidden"));
    }

    @Test
    public void attachExistingElementWithAttributeValue_elementIsDisabled() {
        DisabledElementTemplate template = new DisabledElementTemplate(service);

        Assert.assertTrue(template.label.hasAttribute("id"));
        Assert.assertFalse(template.label.isEnabled());
    }

    @Test
    public void attachExistingElementWithoutChidlrenWithText_elementHasText() {
        ElementWithTextLitTemplate template = new ElementWithTextLitTemplate(
                service);

        Assert.assertEquals("foo bar", template.label.getText());
    }

    @Test
    public void attachExistingElementWithChidlrenWithText_elementHasNoText() {
        ElementWithTextLitTemplate template = new ElementWithTextLitTemplate(
                service);

        Assert.assertEquals("", template.div.getText());
    }

    @Test
    public void attachExistingElementWithAttributeValue_componentSetsPropertyViaCTOR_elementHasPropertyFromTemplate() {
        UI ui = new UI();
        UI.setCurrent(ui);

        VaadinSession session = Mockito.mock(VaadinSession.class);

        ui.getInternals().setSession(session);
        Mockito.when(session.getService()).thenReturn(service);

        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(service.getInstantiator()).thenReturn(instantiator);

        Mockito.when(instantiator.createComponent(ComponentSetProperty.class))
                .thenAnswer(invocation -> new ComponentSetProperty(service));

        InjectComponentWhichSetProperty parent = new InjectComponentWhichSetProperty(
                service);

        Assert.assertEquals("bar",
                parent.child.getElement().getProperty("foo"));
    }

}
