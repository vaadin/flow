package com.vaadin.data.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.PropertyId;
import com.vaadin.tests.server.TestField;

public class ReflectToolsGetSuperFieldTest {

    @Test
    public void getFieldFromSuperClass() {
        class MyClass {
            @PropertyId("testProperty")
            TestField test = new TestField();
        }
        class MySubClass extends MyClass {
            // no fields here
        }

        PropertysetItem item = new PropertysetItem();
        item.addItemProperty("testProperty",
                new ObjectProperty<String>("Value of testProperty"));

        MySubClass form = new MySubClass();

        FieldGroup binder = new FieldGroup(item);
        binder.bindMemberFields(form);

        assertTrue("Value of testProperty".equals(form.test.getValue()));
    }

}
