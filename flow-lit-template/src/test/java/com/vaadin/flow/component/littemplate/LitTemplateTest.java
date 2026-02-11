/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LitTemplateTest {

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
        private Element label;

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

    // This is for checking LitTemplate implements HasStyle
    @Tag("custom-element-with-class")
    private static class ElementWithStyleClass extends LitTemplate {
        ElementWithStyleClass() {
            // Should be possible to modify the class name
            addClassName("custom-element-class-name");
        }
    }

    @BeforeEach
    public void setUp() {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
    }

    @Test
    public void attachExistingElementWithAttributeValue_elementHasAttribute() {
        TestLitTemplate template = new TestLitTemplate(service);

        Assertions.assertTrue(template.label.hasAttribute("id"));
        Assertions.assertEquals("labelId", template.label.getAttribute("id"));

        Assertions.assertTrue(template.label.hasProperty("someattribute"));
        Assertions.assertNotNull(template.label.getProperty("someattribute"));
        Assertions.assertEquals(Boolean.TRUE.toString(),
                template.label.getProperty("someattribute"));

        Assertions.assertFalse(template.label.hasProperty("property-binding"));
        Assertions.assertFalse(template.label.hasProperty("propertyBinding"));

        Assertions.assertFalse(template.label.hasProperty("another-binding"));
        Assertions.assertFalse(template.label.hasProperty("anotherBinding"));

        Assertions.assertFalse(template.label.hasProperty("attribute-binding"));
        Assertions.assertFalse(template.label.hasProperty("attributeBinding"));
        Assertions
                .assertFalse(template.label.hasProperty("attribute-binding$"));
        Assertions.assertFalse(template.label.hasProperty("attributeBinding$"));

        Assertions.assertTrue(template.label.hasProperty("another-attribute"));
        Assertions.assertEquals("baz",
                template.label.getProperty("another-attribute"));

        Assertions.assertTrue(template.label.hasAttribute("hidden"));
        Assertions.assertEquals(Boolean.TRUE.toString(),
                template.label.getAttribute("hidden"));
    }

    @Test
    public void attachExistingElementWithDisabledAttributeValue_exceptionIsThrown() {
        IllegalAttributeException ex = assertThrows(
                IllegalAttributeException.class,
                () -> new DisabledElementTemplate(service));
        assertTrue(
                ex.getMessage().contains("element 'label' with id 'labelId'"));
    }

    @Test
    public void attachExistingElementWithoutChildrenWithText_elementHasNoText() {
        ElementWithTextLitTemplate template = new ElementWithTextLitTemplate(
                service);

        // see #10106
        Assertions.assertEquals("", template.label.getText());
    }

    @Test
    public void attachExistingElementWithChildrenWithText_elementHasNoText() {
        ElementWithTextLitTemplate template = new ElementWithTextLitTemplate(
                service);

        Assertions.assertEquals("", template.div.getText());
    }

}
