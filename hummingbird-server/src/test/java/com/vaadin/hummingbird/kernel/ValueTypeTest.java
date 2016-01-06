package com.vaadin.hummingbird.kernel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.TemplateModelTest.MyTestModel;
import com.vaadin.hummingbird.kernel.ValueType.ArrayType;
import com.vaadin.hummingbird.kernel.ValueType.ObjectType;

public class ValueTypeTest {

    private ObjectType simpleObjectType = ValueType
            .get(Collections.singletonMap("a", ValueType.STRING));

    private ArrayType simpleArrayType = ValueType
            .get(simpleObjectType.getPropertyTypes(), simpleObjectType);

    @Test
    public void testPredefinedIds() {
        Assert.assertEquals("Id is not sync with client code", 0,
                ValueType.STRING.getId());
        Assert.assertEquals("Id is not sync with client code", 1,
                ValueType.BOOLEAN.getId());
        Assert.assertEquals("Id is not sync with client code", 2,
                ValueType.INTEGER.getId());
        Assert.assertEquals("Id is not sync with client code", 3,
                ValueType.NUMBER.getId());
        Assert.assertEquals("Id is not sync with client code", 4,
                ValueType.UNDEFINED.getId());
    }

    @Test
    public void testObjectTypeIdentity() {
        Assert.assertSame(simpleObjectType,
                ValueType.get(Collections.singletonMap("a", ValueType.STRING)));

        Assert.assertNotSame(simpleObjectType,
                ValueType.get(Collections.singletonMap("b", ValueType.STRING)));
        Assert.assertNotSame(simpleObjectType, ValueType
                .get(Collections.singletonMap("a", ValueType.BOOLEAN)));
    }

    @Test
    public void testObjectTypeDefensiveCopy() {
        Map<Object, ValueType> propertyTypes = new HashMap<>();
        propertyTypes.put("unique", ValueType.STRING);
        ObjectType valueType = ValueType.get(propertyTypes);

        Assert.assertEquals(propertyTypes, valueType.getPropertyTypes());

        propertyTypes.put("b", ValueType.INTEGER);

        Assert.assertNotEquals(propertyTypes, valueType.getPropertyTypes());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testObjectTypeImmutable() {
        simpleObjectType.getPropertyTypes().put("b", ValueType.BOOLEAN);
    }

    @Test
    public void testArrayTypeIdentity() {
        Assert.assertSame(simpleArrayType, ValueType
                .get(simpleObjectType.getPropertyTypes(), simpleObjectType));

        Assert.assertNotSame(simpleArrayType, ValueType
                .get(Collections.singletonMap("a", ValueType.STRING), ValueType
                        .get(Collections.singletonMap("b", ValueType.STRING))));
        Assert.assertNotSame(simpleObjectType,
                ValueType.get(Collections.singletonMap("b", ValueType.STRING),
                        simpleObjectType));
        Assert.assertNotSame(simpleObjectType,
                ValueType.get(Collections.singletonMap("a", ValueType.BOOLEAN),
                        simpleObjectType));
    }

    @Test
    public void testArrayTypeDefensiveCopy() {
        Map<Object, ValueType> propertyTypes = new HashMap<>();
        propertyTypes.put("unique2", ValueType.STRING);
        ObjectType valueType = ValueType.get(propertyTypes, simpleObjectType);

        Assert.assertEquals(propertyTypes, valueType.getPropertyTypes());

        propertyTypes.put("b", ValueType.INTEGER);

        Assert.assertNotEquals(propertyTypes, valueType.getPropertyTypes());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testArrayTypeImmutable() {
        simpleArrayType.getPropertyTypes().put("b", ValueType.BOOLEAN);
    }

    @Test
    public void testPrimitiveTypesFromClass() {
        Assert.assertSame(ValueType.STRING, ValueType.get(String.class));

        Assert.assertSame(ValueType.BOOLEAN, ValueType.get(boolean.class));
        Assert.assertSame(ValueType.BOOLEAN, ValueType.get(Boolean.class));

        Assert.assertSame(ValueType.INTEGER, ValueType.get(int.class));
        Assert.assertSame(ValueType.INTEGER, ValueType.get(Integer.class));

        Assert.assertSame(ValueType.NUMBER, ValueType.get(double.class));
        Assert.assertSame(ValueType.NUMBER, ValueType.get(Double.class));
    }

    @Test
    public void testObjectTypeFromBeanType() {
        ObjectType type = (ObjectType) ValueType.get(SimpleBean.class);

        Assert.assertSame(type, ValueType.get(SimpleBean.class));

        Assert.assertSame("No subclass expected", ObjectType.class,
                type.getClass());

        Assert.assertEquals(Collections.singletonMap("value", ValueType.STRING),
                type.getPropertyTypes());
    }

    @Test
    public void testArrayTypeFromParemeterizedList() {
        ObjectType modelType = (ObjectType) ValueType.get(MyTestModel.class);

        ArrayType arrayType = (ArrayType) modelType.getPropertyTypes()
                .get("simpleList");

        Assert.assertEquals(Collections.emptyMap(),
                arrayType.getPropertyTypes());
        Assert.assertSame(ValueType.STRING, arrayType.getMemberType());
    }

    @Test
    public void testArrayTypeForRawList() {
        ValueType type = ValueType.get(List.class);

        Assert.assertSame(ArrayType.class, type.getClass());

        ArrayType arrayType = (ArrayType) type;
        Assert.assertEquals(Collections.emptyMap(),
                arrayType.getPropertyTypes());
        Assert.assertSame(ValueType.UNDEFINED, arrayType.getMemberType());
    }

}
