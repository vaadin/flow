package com.vaadin.flow.server.frontend.scanner;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FrontendAnnotatedClassVisitorTest {

    private FrontendAnnotatedClassVisitor visitor;

    @Before
    public void setup() {
        ClassFinder finder = new ClassFinder.DefaultClassFinder(
                getClass().getClassLoader(), TestAnnotation.class,
                AnnotatedClass.class, ValuesForKey.class, DefaultValues.class);
        visitor = new FrontendAnnotatedClassVisitor(finder,
                TestAnnotation.class.getName());
    }

    @Test
    public void getValues_noAnnotationsFound_returnEmptySet() {
        // No class visited yet, so no annotations processed
        Set<String> values = visitor.getValues("value");
        assertTrue("Values set should be empty", values.isEmpty());
        values = visitor.getValues("other");
        assertTrue("Others set should be empty", values.isEmpty());
    }

    @Test
    public void getValuesForKey_noAnnotationsFound_returnEmptySet() {
        // No class visited yet, so no annotations processed
        Set<String> values = visitor.getValuesForKey("value", "Bar", "other");
        assertTrue("Values set should be empty", values.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getValue_noAnnotationsFound_shouldThrowException() {
        // No annotations found, so getValue should throw an exception
        visitor.getValue("value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getValue_multipleAnnotationsFound_shouldThrowException() {
        // Multiple annotations found, so getValue should throw an exception
        visitor.visitClass(AnnotatedClass.class.getName());
        visitor.getValue("value");
    }

    @Test
    public void getValues_defaultValues_returnsEmptySet() {
        // Default values are ignored by getValues, only explicitly defined
        // attributes are returned
        visitor.visitClass(DefaultValues.class.getName());
        assertTrue(visitor.getValues("value").isEmpty());
        assertTrue(visitor.getValues("other").isEmpty());
    }

    @Test
    public void getValues_returnsOrderedSet() {
        visitor.visitClass(AnnotatedClass.class.getName());

        List<String> values = new ArrayList<>(visitor.getValues("value"));
        assertEquals(List.of("Bar", "Baz", "OnlyValue"), values);

        values = new ArrayList<>(visitor.getValues("other"));
        assertEquals(List.of("aa", "bb", "OnlyOther"), values);

        visitor.visitClass(ValuesForKey.class.getName());

        values = new ArrayList<>(visitor.getValues("value"));
        assertEquals(List.of("Bar", "Baz", "OnlyValue", "Hey", "Ho"), values);

        values = new ArrayList<>(visitor.getValues("other"));
        assertEquals(
                List.of("aa", "bb", "OnlyOther", "zz", "11", "yy", "22", "xx"),
                values);
    }

    @Test
    public void getValuesForKey_returnsMatchingValues() {
        visitor.visitClass(ValuesForKey.class.getName());

        List<String> values = new ArrayList<>(
                visitor.getValuesForKey("value", "Hey", "other"));
        assertEquals(List.of("zz", "yy", "xx"), values);
        values = new ArrayList<>(
                visitor.getValuesForKey("value", "Ho", "other"));
        assertEquals(List.of("11", "22"), values);
    }

    @Test
    public void getValuesForKey_defaultValues_returnsEmptySet() {
        visitor.visitClass(DefaultValues.class.getName());
        assertTrue(
                visitor.getValuesForKey("value", "default", "other").isEmpty());
    }

    @Test
    public void getValue_attributeNotDefined_returnsDefaultValue() {
        visitor.visitClass(DefaultValues.class.getName());
        assertEquals("default", visitor.getValue("value"));
        assertEquals("other", visitor.getValue("other"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @Repeatable(TestAnnotation.Container.class)
    private @interface TestAnnotation {
        String value() default "default";

        String other() default "other";

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        @Inherited
        @Documented
        @interface Container {
            TestAnnotation[] value();
        }
    }

    @TestAnnotation(value = "Bar", other = "aa")
    @TestAnnotation(value = "Baz", other = "bb")
    @TestAnnotation(value = "OnlyValue")
    @TestAnnotation(other = "OnlyOther")
    @TestAnnotation
    private static class AnnotatedClass {
    }

    @TestAnnotation(value = "Hey", other = "zz")
    @TestAnnotation(value = "Ho", other = "11")
    @TestAnnotation(value = "Hey", other = "yy")
    @TestAnnotation(value = "Ho", other = "22")
    @TestAnnotation(value = "Hey", other = "xx")
    private static class ValuesForKey {
    }

    @TestAnnotation
    private static class DefaultValues {
    }

}
