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

package com.vaadin.ui.polymertemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.ui.Tag;
import com.vaadin.util.HasCurrentService;

import net.jcip.annotations.NotThreadSafe;

/**
 * @author Vaadin Ltd.
 */
@NotThreadSafe
public class TemplateInitializerTest extends HasCurrentService {
    private TemplateParser templateParser;

    @Tag("template-initializer-test")
    public class InTemplateClass extends PolymerTemplate<TemplateModel> {
        @Id("inTemplate")
        public Element element;

        public InTemplateClass() {
            super(templateParser);
        }
    }

    @Tag("template-initializer-test")
    public class OutsideTemplateClass extends PolymerTemplate<TemplateModel> {
        @Id("outsideTemplate")
        public Element element;

        public OutsideTemplateClass() {
            super(templateParser);
        }
    }

    @Override
    protected VaadinService createService() {
        VaadinService service = mock(VaadinService.class);
        DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);
        when(configuration.isProductionMode()).thenReturn(false);
        when(service.getDeploymentConfiguration()).thenReturn(configuration);

        return service;
    }

    @Before
    public void setUp() throws NoSuchFieldException {
        String parentTemplateId = InTemplateClass.class.getAnnotation(Tag.class)
                .value();
        assertThat("Both classes should have the same '@Tag' annotation",
                OutsideTemplateClass.class.getAnnotation(Tag.class).value(),
                is(parentTemplateId));

        String inTemplateElementId = InTemplateClass.class.getField("element")
                .getAnnotation(Id.class).value();
        String outsideTemplateElementId = OutsideTemplateClass.class
                .getField("element").getAnnotation(Id.class).value();

        templateParser = (clazz, tag) -> Jsoup.parse(String.format(
                "<dom-module id='%s'><template>"
                        + "    <template><div id='%s'>Test</div></template>"
                        + "    <div id='%s'></div>"
                        + "</template></dom-module>",
                parentTemplateId, inTemplateElementId,
                outsideTemplateElementId));
    }

    @Test(expected = IllegalStateException.class)
    public void inTemplateShouldThrowAnException() {
        new TemplateInitializer(new InTemplateClass(), templateParser)
                .initChildElements();
    }

    @Test
    public void outsideTemplateShouldNotThrowAnException() {
        new TemplateInitializer(new OutsideTemplateClass(), templateParser)
                .initChildElements();
    }

}
