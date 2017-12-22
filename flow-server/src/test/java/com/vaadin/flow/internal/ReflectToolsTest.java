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
package com.vaadin.flow.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
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

    public static class OkToCreate {

    }

    public static class VarArgsCtor {

        public VarArgsCtor(String... args) {

        }
    }

    @Test
    public void testCreateInstance() {
        OkToCreate instance = ReflectTools.createInstance(OkToCreate.class);

        Assert.assertNotNull(instance);
        Assert.assertSame("Created instance should be of the requested type",
                OkToCreate.class, instance.getClass());
    }

    @Test
    public void testCreateInstance_varArgsCtor() {
        VarArgsCtor instance = ReflectTools.createInstance(VarArgsCtor.class);

        Assert.assertNotNull(instance);
        Assert.assertSame("Created instance should be of the requested type",
                VarArgsCtor.class, instance.getClass());
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

    @Test
    public void localClass() {
        class LocalClass {
        }
        assertError(ReflectTools.CREATE_INSTANCE_FAILED_LOCAL_CLASS,
                LocalClass.class);
    }

    @Test
    public void createProxyForNonStaticInnerClass() {
        Class<NonStaticInnerClass> originalClass = NonStaticInnerClass.class;
        Class<?> proxyClass = createProxyClass(originalClass);

        // Even though proxyClass was created on top of NonStaticInnerClass, the
        // exception message does not show it.
        // It's sort of a feature, because proxy class is created as a top-level
        // class.
        assertError(
                ReflectTools.CREATE_INSTANCE_FAILED_NO_PUBLIC_NOARG_CONSTRUCTOR,
                proxyClass);

        // This is how you get correct exception message.
        try {
            ReflectTools.createProxyInstance(proxyClass, originalClass);
            Assert.fail("Creation should cause an exception");
        } catch (IllegalArgumentException re) {
            Assert.assertEquals(String.format(
                    ReflectTools.CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS,
                    originalClass.getName()), re.getMessage());
        }
    }

    public interface TestInterface<T> {

    }

    public static class HasInterface implements TestInterface<String> {
    }

    public static class ParentInterface implements TestInterface<Boolean> {
    }

    public static class ChildInterface extends ParentInterface {
    }

    @Test
    public void getGenericInterfaceClass() {
        Class<?> genericInterfaceType = ReflectTools
                .getGenericInterfaceType(HasInterface.class,
                        TestInterface.class);

        Assert.assertEquals(String.class, genericInterfaceType);

        genericInterfaceType = ReflectTools
                .getGenericInterfaceType(ChildInterface.class,
                        TestInterface.class);

        Assert.assertEquals(Boolean.class, genericInterfaceType);
    }

    private Class<?> createProxyClass(Class<?> originalClass) {
        return new ByteBuddy().subclass(originalClass).make()
                .load(originalClass.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }

    private void assertError(String expectedError, Class<?> cls) {
        try {
            ReflectTools.createInstance(cls);
            Assert.fail("Creation should cause an exception");
        } catch (IllegalArgumentException re) {
            Assert.assertEquals(String.format(expectedError, cls.getName()),
                    re.getMessage());
        }
    }
}
