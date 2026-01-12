/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import org.jsoup.Jsoup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
@NotThreadSafe
public class TemplateInitializerTest {
    private TemplateParser templateParser;
    private VaadinService service;

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

    @Before
    public void setUp() throws Exception {
        service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        String parentTemplateId = InTemplateClass.class.getAnnotation(Tag.class)
                .value();
        assertThat("Both classes should have the same '@Tag' annotation",
                OutsideTemplateClass.class.getAnnotation(Tag.class).value(),
                is(parentTemplateId));

        String inTemplateElementId = InTemplateClass.class.getField("element")
                .getAnnotation(Id.class).value();
        String outsideTemplateElementId = OutsideTemplateClass.class
                .getField("element").getAnnotation(Id.class).value();

        templateParser = (clazz, tag, service) -> new TemplateData("",
                Jsoup.parse(String.format("<dom-module id='%s'><template>"
                        + "    <template><div id='%s'>Test</div></template>"
                        + "    <div id='%s'></div>"
                        + "    <div a='{{twoWay}}' b='{{invalid}} syntax' c='{{two.way}}'"
                        + "        d='{{invalidSyntax' e='{{withEvent::eventName}}' f='[[oneWay]]'></div>"
                        + "</template></dom-module>", parentTemplateId,
                        inTemplateElementId, outsideTemplateElementId)));
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test(expected = IllegalStateException.class)
    public void inTemplateShouldThrowAnException() {
        new TemplateInitializer(new InTemplateClass(), templateParser, service);
    }

    @Test
    public void outsideTemplateShouldNotThrowAnException() {
        new TemplateInitializer(new OutsideTemplateClass(), templateParser,
                service);
    }

    @Test
    public void twoWayBindingPaths() {
        Set<String> twoWayBindingPaths = new TemplateInitializer(
                new OutsideTemplateClass(), templateParser, service)
                .getTwoWayBindingPaths();

        Assert.assertEquals(
                new HashSet<>(Arrays.asList("twoWay", "two.way", "withEvent")),
                twoWayBindingPaths);
    }

}
