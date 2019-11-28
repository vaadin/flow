/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.templatemodel;

import org.hamcrest.CoreMatchers;
import org.jsoup.Jsoup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.HasCurrentService;
import com.vaadin.flow.server.VaadinService;

public class KnownUnsupportedTypesTest extends HasCurrentService {

    @Rule
    public ExpectedException exception = ExpectedException.none();

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
    public void long_throwUnsupportedTypeException() {
        expectUnsupportedTypeException(Long.class);
        new LongTemplate();
    }

    @Test
    public void short_throwUnsupportedTypeException() {
        expectUnsupportedTypeException(Short.class);
        new ShortTemplate();
    }

    @Test
    public void float_throwUnsupportedTypeException() {
        expectUnsupportedTypeException(Float.class);
        new FloatTemplate();
    }

    @Test
    public void byte_throwUnsupportedTypeException() {
        expectUnsupportedTypeException(Byte.class);
        new ByteTemplate();
    }

    @Test
    public void char_throwUnsupportedTypeException() {
        expectUnsupportedTypeException(Character.class);
        new CharTemplate();
    }

    private void expectUnsupportedTypeException(Class<?> clazz) {
        exception.expect(InvalidTemplateModelException.class);
        exception.expectMessage(CoreMatchers.allOf(
                CoreMatchers.containsString(clazz.getName()),
                CoreMatchers.containsString("is not supported"), CoreMatchers
                        .containsString("@" + Encode.class.getSimpleName())));
    }
}
