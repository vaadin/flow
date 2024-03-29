package com.vaadin.flow.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ReflectToolsGetFieldValueByTypeTest {
    @Test
    public void getFieldValue() {
        class MyClass {
            @SuppressWarnings("unused")
            public Integer getField() {
                return 1;
            }

            @SuppressWarnings("unused")
            public void setField(Integer i) {
            }

        }
        class MySubClass extends MyClass {
            @SuppressWarnings("unused")
            public String field = "Hello";
        }

        MySubClass myInstance = new MySubClass();

        java.lang.reflect.Field memberField;
        Object fieldValue = Boolean.FALSE;
        try {
            memberField = myInstance.getClass().getField("field");
            // Should get a String value. Without the third parameter (calling
            // ReflectTools.getJavaFieldValue(Object object, Field field)) would
            // get an Integer value
            fieldValue = ReflectTools.getJavaFieldValue(myInstance, memberField,
                    String.class);
        } catch (Exception e) {
        }
        assertTrue(fieldValue instanceof String);

    }

    @Test
    public void getFieldValueViaGetter() {
        class MyClass {
            @SuppressWarnings("unused")
            public Integer field = 1;
        }
        class MySubClass extends MyClass {
            @SuppressWarnings("unused")
            public String field = "Hello";
        }

        MySubClass myInstance = new MySubClass();

        java.lang.reflect.Field memberField;
        try {
            memberField = myInstance.getClass().getField("field");
            // Should throw an IllegalArgument exception as the mySubClass class
            // doesn't have an Integer field.
            ReflectTools.getJavaFieldValue(myInstance, memberField,
                    Integer.class);
            fail("Previous method call should have thrown an exception");
        } catch (Exception e) {
        }
    }
}
