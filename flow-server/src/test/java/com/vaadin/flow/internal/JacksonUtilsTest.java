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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

public class JacksonUtilsTest {
    ObjectMapper mapper = JacksonUtils.getMapper();

    @Test
    public void testEquals() {
        // Equal
        Assert.assertTrue(JacksonUtils.jsonEquals(mapper.valueToTree(true),
                mapper.valueToTree(true)));
        Assert.assertTrue(JacksonUtils.jsonEquals(mapper.valueToTree("foo"),
                mapper.valueToTree("foo")));
        Assert.assertTrue(JacksonUtils.jsonEquals(mapper.valueToTree(3.14),
                mapper.valueToTree(3.14)));
        Assert.assertTrue(
                JacksonUtils.jsonEquals(mapper.nullNode(), mapper.nullNode()));
        Assert.assertTrue(JacksonUtils.jsonEquals(createTestObject1(),
                createTestObject1()));
        Assert.assertTrue(JacksonUtils.jsonEquals(createTestArray1(),
                createTestArray1()));

        // Non-equal with matching types
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(true),
                mapper.valueToTree(false)));
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree("foo"),
                mapper.valueToTree("oof")));
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(3.14),
                mapper.valueToTree(3.142)));
        Assert.assertFalse(JacksonUtils.jsonEquals(createTestObject1(),
                createTestObject2()));
        Assert.assertFalse(JacksonUtils.jsonEquals(createTestArray1(),
                createTestArray2()));

        // Non-equal with different types
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(true),
                mapper.valueToTree("true")));
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(3.14),
                mapper.valueToTree("3.14")));
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.nullNode(),
                mapper.valueToTree("null")));
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.createObjectNode(),
                mapper.valueToTree("{}")));
        Assert.assertFalse(JacksonUtils.jsonEquals(mapper.createArrayNode(),
                mapper.valueToTree(0)));
        Assert.assertFalse(JacksonUtils.jsonEquals(createTestArray1(),
                createTestObject1()));
    }

    @Test(expected = AssertionError.class)
    public void testEquals_firstNull_throws() {
        JacksonUtils.jsonEquals(null, mapper.nullNode());
    }

    @Test(expected = AssertionError.class)
    public void testEquals_secondNull_throws() {
        JacksonUtils.jsonEquals(mapper.nullNode(), null);
    }

    private ObjectNode createTestObject1() {
        ObjectNode object = mapper.createObjectNode();

        object.put("foo", "foo");
        object.set("bar", createTestArray1());
        object.set("baz", mapper.createObjectNode());

        return object;
    }

    private ObjectNode createTestObject2() {
        ObjectNode object = mapper.createObjectNode();

        object.put("foo", "oof");
        object.set("bar", createTestArray2());
        object.set("baz", mapper.createArrayNode());

        return object;
    }

    private ArrayNode createTestArray1() {
        return Stream
                .of((JsonNode) mapper.valueToTree("foo"),
                        mapper.createObjectNode())
                .collect(JacksonUtils.asArray());
    }

    private ArrayNode createTestArray2() {
        return Stream
                .of((JsonNode) mapper.valueToTree("bar"),
                        mapper.createArrayNode())
                .collect(JacksonUtils.asArray());
    }

    @Test
    public void collectEmptyStream() {
        Stream<JsonNode> jsonValueStream = Stream.empty();
        ArrayNode a = jsonValueStream.collect(JacksonUtils.asArray());
        Assert.assertEquals(0, a.size());
    }

    @Test
    public void testStream() {
        ArrayNode array = createTestArray1();
        List<JsonNode> list = JacksonUtils.stream(array).toList();

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("foo", list.get(0).textValue());
        Assert.assertTrue(JacksonUtils.jsonEquals(list.get(1),
                mapper.createObjectNode()));
    }

    @Test
    public void testObjectStream() {
        ArrayNode array = Stream.of(mapper.createObjectNode(),
                createTestObject1(), createTestObject2())
                .collect(JacksonUtils.asArray());

        List<JsonNode> objects = JacksonUtils.objectStream(array).toList();

        Assert.assertEquals(3, objects.size());
        Assert.assertTrue(JacksonUtils.jsonEquals(mapper.createObjectNode(),
                objects.get(0)));
        Assert.assertTrue(
                JacksonUtils.jsonEquals(createTestObject1(), objects.get(1)));
        Assert.assertTrue(
                JacksonUtils.jsonEquals(createTestObject2(), objects.get(2)));
    }

    @Test
    public void testNumberStream() {
        double[] values = new double[] { 3.14, 42, Double.MAX_VALUE };

        ArrayNode array = DoubleStream.of(values)
                .mapToObj(obj -> mapper.valueToTree(obj))
                .map(obj -> (DoubleNode) obj).collect(JacksonUtils.asArray());

        DoubleStream numberStream = JacksonUtils.numberStream(array);

        Assert.assertArrayEquals(values, numberStream.toArray(), 0);
    }

    @Test
    public void testAsArray() {
        Stream<JsonNode> stream = JacksonUtils.stream(createTestArray1());

        ArrayNode array = stream.collect(JacksonUtils.asArray());

        Assert.assertTrue(JacksonUtils.jsonEquals(createTestArray1(), array));
    }

    @Test
    public void testCreateArray() {
        ArrayNode array = JacksonUtils.createArray(mapper.valueToTree("string"),
                mapper.nullNode());

        Assert.assertEquals(2, array.size());
        Assert.assertEquals("string", array.get(0).asText());
        Assert.assertTrue(array.get(1).isNull());
    }

    @Test
    public void testCreateEmptyArray() {
        ArrayNode array = JacksonUtils.createArray();

        Assert.assertEquals(0, array.size());
    }

    @Test
    public void createObject() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("integer", Integer.valueOf(3));
        map.put("string", "foo");

        ObjectNode object = JacksonUtils.createObject(map, item -> {
            if (item instanceof Integer) {
                return mapper.valueToTree(((Integer) item).doubleValue());
            } else {
                return mapper.valueToTree(String.valueOf(item));
            }
        });

        Assert.assertEquals(2, JacksonUtils.getKeys(object).size());
        Assert.assertEquals(3, object.get("integer").asInt(), 0);
        Assert.assertEquals("foo", object.get("string").asText());
    }

    @Test
    public void testCreateEmptyObject() {
        ObjectNode object = JacksonUtils.createObject(Collections.emptyMap(),
                item -> {
                    throw new AssertionError("Callback should not be called");
                });

        Assert.assertEquals(0, JacksonUtils.getKeys(object).size());
    }

    public static class SimpleBean {
        private String string = "value";
        private int number = 1;

        private float flt = 2.3f;
        private double dbl = 4.56;

        public String getString() {
            return string;
        }

        public int getNumber() {
            return number;
        }

        public float getFlt() {
            return flt;
        }

        public double getDbl() {
            return dbl;
        }
    }

    public static class ParentBean {
        private String parentValue = "parent";
        private ChildBean child = new ChildBean();

        public String getParentValue() {
            return parentValue;
        }

        public ChildBean getChild() {
            return child;
        }
    }

    public static class ChildBean {
        private String childValue = "child";

        public String getChildValue() {
            return childValue;
        }
    }

    public static class BeanWithTemporalFields {

        public LocalTime localTime = LocalTime.of(10, 23, 55);

        public LocalDate localDate = LocalDate.of(2024, 6, 26);

        public LocalDateTime localDateTime = localDate.atTime(localTime);

        public java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);

        public Date date = new Date(sqlDate.getTime());

        public ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime,
                ZoneId.of("Europe/Rome"));

        public Duration duration = Duration.ofSeconds(10);
    }

    public static class ListAndMapBean {
        private Map<String, Integer> integerMap = new HashMap<>();
        private Map<String, ChildBean> childBeanMap = new HashMap<>();

        private List<Integer> integerList = new ArrayList<>();
        private List<ChildBean> childBeanList = new ArrayList<>();
        {
            integerMap.put("one", 1);
            integerMap.put("two", 2);

            integerList.add(3);
            integerList.add(2);
            integerList.add(1);

            ChildBean firstChild = new ChildBean();
            firstChild.childValue = "firstChildValue";
            childBeanMap.put("First", firstChild);
            ChildBean secondChild = new ChildBean();
            secondChild.childValue = "secondChildValue";
            childBeanMap.put("Second", secondChild);

            childBeanList.add(firstChild);
            childBeanList.add(secondChild);
        }

        // these getters are needed for bean serialization:
        public Map<String, Integer> getIntegerMap() {
            return integerMap;
        }

        public List<ChildBean> getChildBeanList() {
            return childBeanList;
        }

        public List<Integer> getIntegerList() {
            return integerList;
        }

        public Map<String, ChildBean> getChildBeanMap() {
            return childBeanMap;
        }
    }

    @Test
    public void simpleBeanToJson() {
        ObjectNode json = JacksonUtils.beanToJson(new SimpleBean());
        Assert.assertEquals("value", json.get("string").asText());
        Assert.assertEquals(1.0, json.get("number").asDouble(), 0.0);
        Assert.assertEquals(2.3f, json.get("flt").floatValue(), 0.0);
        Assert.assertEquals(4.56, json.get("dbl").asDouble(), 0.0);
    }

    @Test
    public void nestedBeanToJson() {
        ObjectNode json = JacksonUtils.beanToJson(new ParentBean());
        Assert.assertEquals("parent", json.get("parentValue").asText());
        JsonNode child = json.get("child");
        Assert.assertEquals("child", child.get("childValue").asText());
    }

    @Test
    public void beanWithTimeFields() {
        BeanWithTemporalFields bean = new BeanWithTemporalFields();
        ObjectNode json = JacksonUtils.beanToJson(bean);

        Assert.assertTrue("LocalTime not serialized as expected",
                JacksonUtils.jsonEquals(JacksonUtils.createNode("10:23:55"),
                        json.get("localTime")));
        Assert.assertTrue("LocalDate not serialized as expected",
                JacksonUtils.jsonEquals(JacksonUtils.createNode("2024-06-26"),
                        json.get("localDate")));
        Assert.assertTrue("LocalDateTime not serialized as expected",
                JacksonUtils.jsonEquals(
                        JacksonUtils.createNode("2024-06-26T10:23:55"),
                        json.get("localDateTime")));
        Assert.assertEquals("ZonedDateTime not serialized as expected",
                bean.zonedDateTime.toEpochSecond(),
                ZonedDateTime.parse(json.get("zonedDateTime").asString())
                        .toEpochSecond(),
                0);
        Assert.assertEquals("ZonedDateTime not serialized as expected",
                bean.sqlDate.getTime(),
                ZonedDateTime.parse(json.get("sqlDate").asString()).toInstant()
                        .toEpochMilli(),
                0);
        Assert.assertEquals("ZonedDateTime not serialized as expected",
                bean.date.getTime(),
                ZonedDateTime.parse(json.get("date").asString()).toInstant()
                        .toEpochMilli(),
                0);
        Assert.assertEquals(10.0,
                Duration.parse(json.get("duration").asString()).toSeconds(), 0);
    }

    @Test
    public void nullChildBean() {
        ParentBean bean = new ParentBean();
        bean.child = null;

        ObjectNode json = JacksonUtils.beanToJson(bean);
        Assert.assertEquals(mapper.nullNode(), json.get("child"));
    }

    @Test
    public void beanWithListAndMap() {
        ListAndMapBean bean = new ListAndMapBean();

        ObjectNode json = JacksonUtils.beanToJson(bean);

        JsonNode integerMap = json.get("integerMap");
        Assert.assertEquals(1, integerMap.get("one").asInt(), 0);
        Assert.assertEquals(2, integerMap.get("two").asInt(), 0);

        JsonNode childBeanMap = json.get("childBeanMap");
        JsonNode firstChild = childBeanMap.get("First");
        Assert.assertEquals("firstChildValue",
                firstChild.get("childValue").asText());
        JsonNode secondChild = childBeanMap.get("Second");
        Assert.assertEquals("secondChildValue",
                secondChild.get("childValue").asText());

        JsonNode integerList = json.get("integerList");
        Assert.assertEquals(3, integerList.get(0).asInt(), 0);
        Assert.assertEquals(2, integerList.get(1).asInt(), 0);
        Assert.assertEquals(1, integerList.get(2).asInt(), 0);

        JsonNode childBeanList = json.get("childBeanList");
        Assert.assertEquals("firstChildValue",
                childBeanList.get(0).get("childValue").asText());
        Assert.assertEquals("secondChildValue",
                childBeanList.get(1).get("childValue").asText());
    }

    @Test
    public void simpleBeanListToJson() {
        ArrayList<SimpleBean> list = new ArrayList<>();
        SimpleBean bean1 = new SimpleBean();
        bean1.string = "bean1";
        SimpleBean bean2 = new SimpleBean();
        bean2.string = "bean2";
        list.add(bean1);
        list.add(bean2);
        ArrayNode json = JacksonUtils.listToJson(list);

        Assert.assertEquals("bean1", json.get(0).get("string").asText());
        Assert.assertEquals("bean2", json.get(1).get("string").asText());
    }

    @Test
    public void simpleMapToJson() {
        Map<String, Object> map = new HashMap<>();
        SimpleBean bean1 = new SimpleBean();
        bean1.string = "bean1";
        SimpleBean bean2 = new SimpleBean();
        bean2.string = "bean2";

        map.put("one", bean1);
        map.put("two", bean2);
        ObjectNode json = JacksonUtils.mapToJson(map);

        Assert.assertEquals("bean1", json.get("one").get("string").asText());
        Assert.assertEquals("bean2", json.get("two").get("string").asText());
    }

    public record Person(String name, double age, boolean canSwim) {

    }

    @Test
    public void testReadObject() {
        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.put("name", "Foo");
        jsonObject.put("age", 30.5);
        jsonObject.put("canSwim", true);

        Person person = JacksonUtils.readToObject(jsonObject, Person.class);

        Assert.assertEquals("Foo", person.name);
        Assert.assertEquals(30.5, person.age, 0.0);
        Assert.assertTrue(person.canSwim);
    }

    @Test
    public void toFileJson() throws JacksonException {
        ObjectNode json = JacksonUtils.beanToJson(new ParentBean());
        Assert.assertEquals("""
                {
                  "child": {
                    "childValue": "child"
                  },
                  "parentValue": "parent"
                }""", JacksonUtils.toFileJson(json).replace("\r\n", "\n"));

    }

}
