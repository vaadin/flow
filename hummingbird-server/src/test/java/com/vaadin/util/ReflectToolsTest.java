/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.util;

import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.Test;

public class ReflectToolsTest {
    public class NonStaticInnerClass {
        public NonStaticInnerClass() {

        }
    }

    private class PrivateInnerClass {
    }

    private static class PrivateStaticInnerClassPublicConstructor {
        @SuppressWarnings("unused")
        public PrivateStaticInnerClassPublicConstructor() {

        }
    }

    public static class StaticInnerPrivateConstructorClass {
        private StaticInnerPrivateConstructorClass() {
        }
    }

    public static class StaticInnerConstructorNeedsParamsClass {
        public StaticInnerConstructorNeedsParamsClass(String foo) {

        }
    }

    public static class ConstructorThrowsExceptionClass {
        public ConstructorThrowsExceptionClass() {
            throw new NullPointerException();
        }
    }

    @Test
    public void createNonStaticInnerClass() {
        assertError(
                ReflectTools.CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS,
                NonStaticInnerClass.class);
    }

    @Test
    public void createPrivateInnerClass() {
        assertError(
                ReflectTools.CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS,
                PrivateInnerClass.class);
    }

    @Test
    public void createPrivateStaticInnerClass() {
        assertError(ReflectTools.CREATE_INSTANCE_FAILED_ACCESS_EXCEPTION,
                PrivateStaticInnerClassPublicConstructor.class);
    }

    @Test
    public void createStaticInnerPrivateConstructorClass() {
        assertError(
                ReflectTools.CREATE_INSTANCE_FAILED_NO_PUBLIC_NOARG_CONSTRUCTOR,
                StaticInnerPrivateConstructorClass.class);
    }

    @Test
    public void createStaticInnerConstructorNeedsParamsClass() {
        assertError(
                ReflectTools.CREATE_INSTANCE_FAILED_NO_PUBLIC_NOARG_CONSTRUCTOR,
                StaticInnerConstructorNeedsParamsClass.class);
    }

    @Test
    public void createConstructorThrowsExceptionClass() {
        assertError(
                ReflectTools.CREATE_INSTANCE_FAILED_CONSTRUCTOR_THREW_EXCEPTION,
                ConstructorThrowsExceptionClass.class);
    }

    private void assertError(String expectedError, Class<?> cls) {
        try {
            ReflectTools.createInstance(cls);
        } catch (RuntimeException re) {
            Assert.assertEquals(
                    MessageFormat.format(expectedError, cls.getName()),
                    re.getMessage());
        }
    }
}
