/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.HasCurrentService;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnownUnsupportedTypesTest extends HasCurrentService {

    public static class LongType implements TemplateModel {

        public Long getSize() {
            return 0l;
        }
    }

    public static class ShortType implements TemplateModel {

        public Short getSize() {
            return 0;
        }
    }

    public static class FloatType implements TemplateModel {

        public Float getSize() {
            return 0f;
        }
    }

    public static class ByteType implements TemplateModel {

        public Byte getSize() {
            return 0;
        }
    }

    public static class CharType implements TemplateModel {

        public Character getSize() {
            return 0;
        }
    }

    @Tag("div")
    public static class EmptyDivTemplate<M extends TemplateModel>
            extends PolymerTemplate<M> {
        public EmptyDivTemplate() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='div'></dom-module>")));
        }

    }

    public static class LongTemplate extends EmptyDivTemplate<LongType> {

    }

    public static class ShortTemplate extends EmptyDivTemplate<ShortType> {

    }

    public static class FloatTemplate extends EmptyDivTemplate<FloatType> {

    }

    public static class ByteTemplate extends EmptyDivTemplate<ByteType> {

    }

    public static class CharTemplate extends EmptyDivTemplate<CharType> {

    }

    @Override
    protected VaadinService createService() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        return service;
    }

    @Test
    void long_throwUnsupportedTypeException() {
        assertUnsupportedTypeException(Long.class, () -> new LongTemplate());
    }

    @Test
    void short_throwUnsupportedTypeException() {
        assertUnsupportedTypeException(Short.class, () -> new ShortTemplate());
    }

    @Test
    void float_throwUnsupportedTypeException() {
        assertUnsupportedTypeException(Float.class, () -> new FloatTemplate());
    }

    @Test
    void byte_throwUnsupportedTypeException() {
        assertUnsupportedTypeException(Byte.class, () -> new ByteTemplate());
    }

    @Test
    void char_throwUnsupportedTypeException() {
        assertUnsupportedTypeException(Character.class,
                () -> new CharTemplate());
    }

    private void assertUnsupportedTypeException(Class<?> clazz,
            Runnable action) {
        InvalidTemplateModelException ex = assertThrows(
                InvalidTemplateModelException.class, () -> action.run());
        String message = ex.getMessage();
        assertTrue(message.contains(clazz.getName()));
        assertTrue(message.contains("is not supported"));
        assertTrue(message.contains("@" + Encode.class.getSimpleName()));
    }
}
