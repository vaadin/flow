package com.vaadin.flow.internal;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.vaadin.flow.internal.ReflectTools;

public class ReflectToolsGetPrimitiveFieldValueTest {
    @Test
    public void getFieldValueViaGetter() {
        class MyClass {
            @SuppressWarnings("unused")
            public int field = 1;
        }

        MyClass myInstance = new MyClass();

        java.lang.reflect.Field memberField;
        Object fieldValue = Boolean.FALSE;
        try {
            memberField = myInstance.getClass().getField("field");
            fieldValue = ReflectTools.getJavaFieldValue(myInstance,
                    memberField);
        } catch (Exception e) {
        }
        assertFalse(fieldValue instanceof Boolean);
    }
}
