/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReflectToolsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public class NonStaticInnerClass {
        public NonStaticInnerClass() {

        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TestAnnotation {
        String value();
    }

    @TestAnnotation("foo")
    public static class ClassWithAnnotation {

    }

    public static class ClassWithoutAnnotation {

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

    public interface Entity<ID> {
        ID getId();

        void setId(ID id);
    }

    public static class CustomClassLoader extends ClassLoader {
        protected CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        protected CustomClassLoader() {
            super();
        }
    }

    public class Category implements Serializable, Entity<Long> {

        @Override
        public Long getId() {
            return null;
        }

        @Override
        public void setId(Long id) {
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

    public interface TestInterfaceMulti<T, R, S> {

    }

    public static class HasInterfaceMulti
            implements TestInterfaceMulti<String, Integer, Double> {
    }

    public static class ParentInterfacePartial<Z>
            implements TestInterfaceMulti<Boolean, Z, Long> {
    }

    public static class ParentInterfaceMulti
            implements TestInterfaceMulti<Boolean, Float, Long> {

    }

    public static class ChildInterfaceMulti extends ParentInterfaceMulti {
    }

    public static class ChildInterfacePartial
            extends ParentInterfacePartial<Short> {
    }

    public static abstract class TestAbstractClass {

    }

    protected static class TestProtectedClass {

    }

    protected static class TestPackageProtectedClass {

    }

    private static class TestPrivateClass {

    }

    public static class NormalService {

    }

    public static class TestNoNonArgConstructorClass {
        public TestNoNonArgConstructorClass(String foo) {

        }
    }

    @Test
    public void getGenericInterfaceClass() {
        Class<?> genericInterfaceType = ReflectTools.getGenericInterfaceType(
                HasInterface.class, TestInterface.class);

        Assert.assertEquals(String.class, genericInterfaceType);

        genericInterfaceType = ReflectTools.getGenericInterfaceType(
                ChildInterface.class, TestInterface.class);

        Assert.assertEquals(Boolean.class, genericInterfaceType);
    }

    @Test
    public void getGenericInterfaceClasses() {

        List<Class<?>> genericInterfaceTypes = ReflectTools
                .getGenericInterfaceTypes(HasInterface.class,
                        TestInterface.class);
        Assert.assertArrayEquals(new Class<?>[] { String.class },
                genericInterfaceTypes.toArray());

        genericInterfaceTypes = ReflectTools.getGenericInterfaceTypes(
                ChildInterface.class, TestInterface.class);
        Assert.assertArrayEquals(new Class<?>[] { Boolean.class },
                genericInterfaceTypes.toArray());

        genericInterfaceTypes = ReflectTools.getGenericInterfaceTypes(
                HasInterfaceMulti.class, TestInterfaceMulti.class);
        Assert.assertArrayEquals(
                new Class<?>[] { String.class, Integer.class, Double.class },
                genericInterfaceTypes.toArray());

        genericInterfaceTypes = ReflectTools.getGenericInterfaceTypes(
                ChildInterfaceMulti.class, TestInterfaceMulti.class);
        Assert.assertArrayEquals(
                new Class<?>[] { Boolean.class, Float.class, Long.class },
                genericInterfaceTypes.toArray());

        genericInterfaceTypes = ReflectTools.getGenericInterfaceTypes(
                ChildInterfacePartial.class, TestInterfaceMulti.class);
        Assert.assertArrayEquals(
                new Class<?>[] { Boolean.class, Short.class, Long.class },
                genericInterfaceTypes.toArray());
    }

    @Test
    public void findCommonBaseType_sameType() {
        assertSame(Number.class,
                ReflectTools.findCommonBaseType(Number.class, Number.class));
    }

    @Test
    public void findCommonBaseType_aExtendsB() {
        assertSame(Number.class,
                ReflectTools.findCommonBaseType(Integer.class, Number.class));
    }

    @Test
    public void findCommonBaseType_bExtendsA() {
        assertSame(Number.class,
                ReflectTools.findCommonBaseType(Number.class, Integer.class));
    }

    @Test
    public void findCommonBaseType_commonBase() {
        assertSame(Number.class,
                ReflectTools.findCommonBaseType(Double.class, Integer.class));
    }

    @Test
    public void findCommonBaseType_noCommonBase() {
        assertSame(Object.class,
                ReflectTools.findCommonBaseType(String.class, Number.class));
    }

    @Test
    public void findCommonBaseType_interfaceNotSupported() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("a cannot be an interface");
        ReflectTools.findCommonBaseType(Comparable.class, Object.class);
    }

    @Test
    public void findCommonBaseType_primitiveNotSupported() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("a cannot be a primitive type");
        ReflectTools.findCommonBaseType(int.class, Object.class);
    }

    @Test
    public void getSetters_classIsGeneric_syntheticMethodsAreFilteredOut() {
        List<Method> setters = ReflectTools.getSetterMethods(Category.class)
                .collect(Collectors.toList());
        Assert.assertEquals(1, setters.size());
        Method setter = setters.get(0);
        Assert.assertEquals("setId", setter.getName());
        Assert.assertEquals(Long.class, setter.getParameterTypes()[0]);
    }

    @Test
    public void findClosestCommonClassLoaderAncestor_findAncestor_whenBothArgumentsAreTheSame() {
        CustomClassLoader loader = new CustomClassLoader();
        ClassLoader ret = ReflectTools
                .findClosestCommonClassLoaderAncestor(loader, loader).get();

        Assert.assertEquals(loader, ret);
    }

    public void findClosestCommonClassLoaderAncestor_null_whenNoSharedAncestor() {
        CustomClassLoader loader1 = new CustomClassLoader();
        CustomClassLoader loader2 = new CustomClassLoader();

        Optional<ClassLoader> ret = ReflectTools
                .findClosestCommonClassLoaderAncestor(loader1, loader2);

        Assert.assertFalse(ret.isPresent());
    }

    @Test
    public void findClosestCommonClassLoaderAncestor_findsAncestor_whenOneIsParentOfTheOther() {
        CustomClassLoader parent = new CustomClassLoader();
        CustomClassLoader child = new CustomClassLoader(parent);
        ClassLoader ret = ReflectTools
                .findClosestCommonClassLoaderAncestor(parent, child).get();

        Assert.assertEquals(parent, ret);
    }

    @Test
    public void findClosestCommonClassLoaderAncestor_findsAncestor_whenLoadersShareParent() {
        CustomClassLoader parent = new CustomClassLoader();
        CustomClassLoader childA = new CustomClassLoader(parent);
        CustomClassLoader childB = new CustomClassLoader(parent);
        ClassLoader ret = ReflectTools
                .findClosestCommonClassLoaderAncestor(childA, childB).get();

        Assert.assertEquals(parent, ret);
    }

    @Test
    public void findClosestCommonClassLoaderAncestor_findsAncestor_whenAncestorsAreOnDifferentLevels() {
        CustomClassLoader grandParent = new CustomClassLoader();
        CustomClassLoader parent = new CustomClassLoader(grandParent);
        CustomClassLoader childA = new CustomClassLoader(parent);
        CustomClassLoader childB = new CustomClassLoader(grandParent);

        ClassLoader ret = ReflectTools
                .findClosestCommonClassLoaderAncestor(childA, childB).get();

        Assert.assertEquals(grandParent, ret);
    }

    @Test
    public void findClosestCommonClassLoaderAncestor_empty_whenEitherOrBothNull() {
        CustomClassLoader loader = new CustomClassLoader();

        Optional<ClassLoader> ret;

        ret = ReflectTools.findClosestCommonClassLoaderAncestor(loader, null);
        Assert.assertFalse(ret.isPresent());

        ret = ReflectTools.findClosestCommonClassLoaderAncestor(null, loader);
        Assert.assertFalse(ret.isPresent());

        ret = ReflectTools.findClosestCommonClassLoaderAncestor(null, null);
        Assert.assertFalse(ret.isPresent());
    }

    @Test
    public void hasAnnotation_annotationPresents_returnsTrue() {
        Assert.assertTrue(ReflectTools.hasAnnotation(ClassWithAnnotation.class,
                TestAnnotation.class.getName()));
    }

    @Test
    public void hasAnnotation_annotationIsAbsent_returnsFalse() {
        Assert.assertFalse(ReflectTools.hasAnnotation(
                ClassWithoutAnnotation.class, TestAnnotation.class.getName()));
    }

    @Test
    public void hasAnnotationWithSimpleName_annotationPresents_returnsTrue() {
        Assert.assertTrue(ReflectTools.hasAnnotationWithSimpleName(
                ClassWithAnnotation.class,
                TestAnnotation.class.getSimpleName()));
    }

    @Test
    public void hasAnnotationWithSimpleName_annotationIsAbsent_returnsFalse() {
        Assert.assertFalse(ReflectTools.hasAnnotationWithSimpleName(
                ClassWithoutAnnotation.class,
                TestAnnotation.class.getSimpleName()));
    }

    @Test
    public void getAnnotationMethodValue_annotaitonHasMethod_theValueIsReturned() {
        Assert.assertEquals("foo", ReflectTools.getAnnotationMethodValue(
                ClassWithAnnotation.class.getAnnotation(TestAnnotation.class),
                "value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAnnotationMethodValue_annotationHasNoMethod_throws() {
        ReflectTools.getAnnotationMethodValue(
                ClassWithAnnotation.class.getAnnotation(TestAnnotation.class),
                "foo");
    }

    @Test
    public void getAnnotation_annotationPresents_returnsAnnotation() {
        Optional<Annotation> annotation = ReflectTools.getAnnotation(
                ClassWithAnnotation.class, TestAnnotation.class.getName());
        Assert.assertTrue(annotation.isPresent());
        Assert.assertEquals(
                ClassWithAnnotation.class.getAnnotation(TestAnnotation.class),
                annotation.get());
    }

    @Test
    public void getAnnotation_annotationIsAbsent_returnsEmpty() {
        Optional<Annotation> annotation = ReflectTools.getAnnotation(
                ClassWithoutAnnotation.class, TestAnnotation.class.getName());
        Assert.assertFalse(annotation.isPresent());
    }

    @Test
    public void intefaceShouldNotBeInstantiableService() {
        assertFalse(ReflectTools.isInstantiableService(TestInterface.class));
    }

    @Test
    public void abstractClassShouldNotBeInstantiableService() {
        assertFalse(
                ReflectTools.isInstantiableService(TestAbstractClass.class));
    }

    @Test
    public void nonPublicClassShouldNotBeInstantiableService() {
        assertFalse(
                ReflectTools.isInstantiableService(TestProtectedClass.class));
        assertFalse(ReflectTools
                .isInstantiableService(TestPackageProtectedClass.class));
        assertFalse(ReflectTools.isInstantiableService(TestPrivateClass.class));
    }

    @Test
    public void ClassWithoutNonArgConstructorShouldNotBeInstantiableService() {
        assertFalse(ReflectTools
                .isInstantiableService(TestNoNonArgConstructorClass.class));
    }

    @Test
    public void nonStaticInnerClassShouldNotBeInstantiableService() {
        assertFalse(
                ReflectTools.isInstantiableService(NonStaticInnerClass.class));
    }

    @Test
    public void privateInnerClassShouldNotBeInstantiableService() {
        assertFalse(
                ReflectTools.isInstantiableService(PrivateInnerClass.class));
    }

    @Test
    public void normalSericieShouldBeInstantiableService() {
        assertTrue(ReflectTools.isInstantiableService(NormalService.class));
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
