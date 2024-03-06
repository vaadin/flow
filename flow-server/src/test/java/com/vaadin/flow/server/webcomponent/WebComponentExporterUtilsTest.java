/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webcomponent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

@SuppressWarnings("rawtypes")
public class WebComponentExporterUtilsTest {

    @Test
    public void getFactories_notEligibleExportersAreFiltered_factoriesAreReturned() {

        Set<WebComponentExporterFactory> factories = WebComponentExporterUtils
                .getFactories(new HashSet<>(Arrays.asList(GoodExporter.class,
                        AbstractExporter.class, PackageLocalExporter.class,
                        NoPublicCtorExporter.class, ExporterFactory.class)));
        Assert.assertEquals(2, factories.size());

        Iterator<WebComponentExporterFactory> iterator = factories.iterator();
        WebComponentExporterFactory factory = iterator.next();
        WebComponentExporterFactory another = iterator.next();
        if (factory instanceof ExporterFactory) {
            assertFactories(factory, another);
        } else {
            assertFactories(another, factory);
        }
    }

    private void assertFactories(WebComponentExporterFactory factory,
            WebComponentExporterFactory anotherFactory) {
        Assert.assertTrue(
                anotherFactory instanceof DefaultWebComponentExporterFactory);
        WebComponentExporter exporter = anotherFactory.create();
        MatcherAssert.assertThat(exporter,
                CoreMatchers.instanceOf(GoodExporter.class));

        exporter = factory.create();
        MatcherAssert.assertThat(exporter,
                CoreMatchers.instanceOf(NoDefaultCtorExporter.class));
    }

    public static class GoodExporter extends WebComponentExporter<Component> {

        public GoodExporter() {
            super("a-b");
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }

    }

    public static abstract class AbstractExporter
            extends WebComponentExporter<Component> {

        public AbstractExporter() {
            super("a-b");
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }

    }

    static abstract class PackageLocalExporter
            extends WebComponentExporter<Component> {

        public PackageLocalExporter() {
            super("a-b");
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }

    }

    public static class NoPublicCtorExporter
            extends WebComponentExporter<Component> {

        NoPublicCtorExporter() {
            super("a-b");
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }

    }

    public static class NoDefaultCtorExporter
            extends WebComponentExporter<Component> {

        public NoDefaultCtorExporter(String tag) {
            super(tag);
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }

    }

    public static class ExporterFactory
            implements WebComponentExporterFactory<Component> {

        @Override
        public WebComponentExporter<Component> create() {
            return new NoDefaultCtorExporter("foo-bar");
        }
    }
}
