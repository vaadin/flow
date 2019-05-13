/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.router;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParameterDeserializerTest {

    @Test
    public void testSimple() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(Simple.class, OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(SimpleAnnotated.class, OptionalParameter.class));
    }

    @Test
    public void testInterface() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(Normal.class, OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(NormalAnnotated.class, OptionalParameter.class));
    }

    @Test
    public void testGenericInterface() {
        assertFalse(ParameterDeserializer.isAnnotatedParameter(Generic.class, OptionalParameter.class));
        assertTrue(ParameterDeserializer.isAnnotatedParameter(GenericAnnotated.class, OptionalParameter.class));
    }

    public static class Simple implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    public static class SimpleAnnotated implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
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
        default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
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
        default void setParameter(BeforeEvent event, @OptionalParameter T parameter) {
        }
    }

    public static class GenericAnnotated implements GenericInterfaceAnnotated<String> {
    }


}