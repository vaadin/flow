/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import net.jcip.annotations.NotThreadSafe;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

import static org.junit.jupiter.api.Assertions.assertThrows;

@NotThreadSafe
class CompositeTest {

    @Tag("div")
    public static class MyTemplate extends PolymerTemplate<TemplateModel> {

        public MyTemplate() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='div'></dom-module>")));
        }
    }

    public static class KeyNotifierComposite extends Composite<MyTemplate>
            implements KeyNotifier {

        @Override
        protected MyTemplate initContent() {
            MyTemplate template = new MyTemplate();

            addKeyUpListener(Key.ENTER, event -> {
            }, KeyModifier.CONTROL);

            return template;
        }
    }

    private VaadinService service;

    @BeforeEach
    void setup() {
        service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
    }

    @AfterEach
    void tearDown() {
        VaadinService.setCurrent(null);
    }

    @Test
    void getContent_compositeIsKeyNotifier() {
        assertThrows(IllegalStateException.class, () -> {
            KeyNotifierComposite composite = new KeyNotifierComposite();
            composite.getContent();
        });
    }

    /*
     * This is just a test for #1181.
     */
    @Test
    // @Disabled("Failing after adding connect client generators")
    void templateInsideComposite_compositeCanBeAdded() {
        class MyComponent extends Composite<MyTemplate> {

        }

        MyComponent component = new MyComponent();

        UI ui = new UI();
        // Doesn't throw any exception
        ui.add(component);
    }

}
