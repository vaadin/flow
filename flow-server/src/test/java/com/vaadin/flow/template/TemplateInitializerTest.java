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

package com.vaadin.flow.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.external.jsoup.Jsoup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.VaadinService;

/**
 * @author Vaadin Ltd.
 */
public class TemplateInitializerTest {
    private TemplateParser templateParser;

    @Tag("template-initializer-test")
    public class DomIfClass extends PolymerTemplate<TemplateModel> {
        @Id("domIf")
        public Element element;

        public DomIfClass() {
            super(templateParser);
        }
    }

    @Tag("template-initializer-test")
    public class DomRepeatClass extends PolymerTemplate<TemplateModel> {
        @Id("domRepeat")
        public Element element;

        public DomRepeatClass() {
            super(templateParser);
        }
    }

    @Tag("template-initializer-test")
    public class NestedTemplateClass extends PolymerTemplate<TemplateModel> {
        @Id("nestedTemplate")
        public Element element;

        public NestedTemplateClass() {
            super(templateParser);
        }
    }

    @Tag("template-initializer-test")
    public class NoInjectClass extends PolymerTemplate<TemplateModel> {

        public NoInjectClass() {
            super(templateParser);
        }
    }

    @BeforeClass
    public static void initVaadinService() {
        VaadinService service = mock(VaadinService.class);
        DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);
        when(configuration.isProductionMode()).thenReturn(false);
        when(service.getDeploymentConfiguration()).thenReturn(configuration);
        VaadinService.setCurrent(service);
    }

    @AfterClass
    public static void removeVaadinService() {
        VaadinService.setCurrent(null);
    }

    @Before
    public void setUp() throws NoSuchFieldException {
        String parentTemplateId = DomIfClass.class.getAnnotation(Tag.class)
                .value();

        for (Class<?> childClass : getClass().getClasses()) {
            assertThat(
                    childClass.getName()
                            + " should have the same '@Tag' annotation",
                    childClass.getAnnotation(Tag.class).value(),
                    is(parentTemplateId));
        }

        String domIfElementId = DomIfClass.class.getField("element")
                .getAnnotation(Id.class).value();
        String domRepeatElementId = DomRepeatClass.class.getField("element")
                .getAnnotation(Id.class).value();
        String nestedTemplateElementId = NestedTemplateClass.class
                .getField("element").getAnnotation(Id.class).value();

        templateParser = (clazz, tag) -> Jsoup.parse(String.format(
                "<dom-module id='%s'><template>"
                        + "    <dom-if><div id='%s'>Test</div></dom-if>"
                        + "    <dom-repeat><div id='%s'>Test</div></dom-repeat>"
                        + "    <vaadin-combo-box><template><div id='%s'>Test</div></template><vaadin-combo-box>"
                        + "</template></dom-module>",
                parentTemplateId, domIfElementId, domRepeatElementId,
                nestedTemplateElementId));
    }

    @Test(expected = IllegalStateException.class)
    public void domIfShouldThrowAnException() {
        new TemplateInitializer(new DomIfClass(), templateParser)
                .initChildElements();
    }

    @Test(expected = IllegalStateException.class)
    public void domRepeatShouldThrowAnException() {
        new TemplateInitializer(new DomRepeatClass(), templateParser)
                .initChildElements();
    }

    @Test(expected = IllegalStateException.class)
    public void nestedTemplateShouldThrowAnException() {
        new TemplateInitializer(new NestedTemplateClass(), templateParser)
                .initChildElements();
    }

    @Test
    public void noInjectShouldNotThrow() {
        new TemplateInitializer(new NoInjectClass(), templateParser)
                .initChildElements();
    }

}
