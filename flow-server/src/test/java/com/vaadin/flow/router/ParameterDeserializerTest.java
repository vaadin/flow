/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParameterDeserializerTest {

    @Test
    public void testSimple() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(Simple.class,
                OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(
                SimpleAnnotated.class, OptionalParameter.class));
    }

    @Test
    public void testInterface() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(Normal.class,
                OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(
                NormalAnnotated.class, OptionalParameter.class));
    }

    @Test
    public void parameterizedViaClass() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(
                ParameterizedViaSuperClass.class, OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(
                ParameterizedAnnotatedViaSuperClass.class,
                OptionalParameter.class));
    }

    @Test
    public void parameterizedViaInterface() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(
                ParameterizedClass.class, OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(
                ParameterizedAnnotatedClass.class, OptionalParameter.class));
    }

    @Test
    public void testGenericInterface() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(Generic.class,
                OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(
                GenericAnnotated.class, OptionalParameter.class));
    }

    public static class Simple implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    public static class ParameterizedSuperClass<T>
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    public static class ParameterizedViaSuperClass<T>
            extends ParameterizedSuperClass<T> {

    }

    public static class ParameterizedAnnotatedViaSuperClass<T>
            extends ParameterizedSuperClass<T> {

        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
        }
    }

    public static class ParameterizedClass<T>
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    public static class ParameterizedAnnotatedClass<T>
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
        }
    }

    public static class SimpleAnnotated implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
        }
    }

    public interface NormalInterface extends HasUrlParameter<String> {
        @Override
        default void setParameter(BeforeEvent event, String parameter) {
        }
    }

    public static class Normal implements NormalInterface {
    }

    public interface NormalInterfaceAnnotated extends HasUrlParameter<String> {
        @Override
        default void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
        }
    }

    public static class NormalAnnotated implements NormalInterfaceAnnotated {
    }

    public interface GenericInterface<T> extends HasUrlParameter<T> {
        @Override
        default void setParameter(BeforeEvent event, T parameter) {
        }
    }

    public static class Generic implements GenericInterface<String> {
    }

    public interface GenericInterfaceAnnotated<T> extends HasUrlParameter<T> {
        @Override
        default void setParameter(BeforeEvent event,
                @OptionalParameter T parameter) {
        }
    }

    public static class GenericAnnotated
            implements GenericInterfaceAnnotated<String> {
    }

}
