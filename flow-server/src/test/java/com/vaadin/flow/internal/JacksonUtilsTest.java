/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JacksonUtilsTest {
    ObjectMapper mapper = JacksonUtils.getMapper();

    @Test
    public void testEquals() {
        // Equal
        Assertions.assertTrue(JacksonUtils.jsonEquals(mapper.valueToTree(true),
                mapper.valueToTree(true)));
        Assertions.assertTrue(JacksonUtils.jsonEquals(mapper.valueToTree("foo"),
                mapper.valueToTree("foo")));
        Assertions.assertTrue(JacksonUtils.jsonEquals(mapper.valueToTree(3.14),
                mapper.valueToTree(3.14)));
        Assertions.assertTrue(
                JacksonUtils.jsonEquals(mapper.nullNode(), mapper.nullNode()));
        Assertions.assertTrue(JacksonUtils.jsonEquals(createTestObject1(),
                createTestObject1()));
        Assertions.assertTrue(JacksonUtils.jsonEquals(createTestArray1(),
                createTestArray1()));

        // Non-equal with matching types
        Assertions.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(true),
                mapper.valueToTree(false)));
        Assertions.assertFalse(JacksonUtils.jsonEquals(
                mapper.valueToTree("foo"), mapper.valueToTree("oof")));
        Assertions.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(3.14),
                mapper.valueToTree(3.142)));
        Assertions.assertFalse(JacksonUtils.jsonEquals(createTestObject1(),
                createTestObject2()));
        Assertions.assertFalse(JacksonUtils.jsonEquals(createTestArray1(),
                createTestArray2()));

        // Non-equal with different types
        Assertions.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(true),
                mapper.valueToTree("true")));
        Assertions.assertFalse(JacksonUtils.jsonEquals(mapper.valueToTree(3.14),
                mapper.valueToTree("3.14")));
        Assertions.assertFalse(JacksonUtils.jsonEquals(mapper.nullNode(),
                mapper.valueToTree("null")));
        Assertions.assertFalse(JacksonUtils.jsonEquals(
                mapper.createObjectNode(), mapper.valueToTree("{}")));
        Assertions.assertFalse(JacksonUtils.jsonEquals(mapper.createArrayNode(),
                mapper.valueToTree(0)));
        Assertions.assertFalse(JacksonUtils.jsonEquals(createTestArray1(),
                createTestObject1()));
    }

    @Test
    public void testEquals_firstNull_throws() {
        assertThrows(AssertionError.class, () -> {
            JacksonUtils.jsonEquals(null, mapper.nullNode());
        });
    }

    @Test
    public void testEquals_secondNull_throws() {
        assertThrows(AssertionError.class, () -> {
            JacksonUtils.jsonEquals(mapper.nullNode(), null);
        });
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
        Assertions.assertEquals(0, a.size());
    }

    @Test
    public void testStream() {
        ArrayNode array = createTestArray1();
        List<JsonNode> list = JacksonUtils.stream(array).toList();

        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("foo", list.get(0).textValue());
        Assertions.assertTrue(JacksonUtils.jsonEquals(list.get(1),
                mapper.createObjectNode()));
    }

    @Test
    public void testObjectStream() {
        ArrayNode array = Stream.of(mapper.createObjectNode(),
                createTestObject1(), createTestObject2())
                .collect(JacksonUtils.asArray());

        List<JsonNode> objects = JacksonUtils.objectStream(array).toList();

        Assertions.assertEquals(3, objects.size());
        Assertions.assertTrue(JacksonUtils.jsonEquals(mapper.createObjectNode(),
                objects.get(0)));
        Assertions.assertTrue(
                JacksonUtils.jsonEquals(createTestObject1(), objects.get(1)));
        Assertions.assertTrue(
                JacksonUtils.jsonEquals(createTestObject2(), objects.get(2)));
    }

    @Test
    public void testNumberStream() {
        double[] values = new double[] { 3.14, 42, Double.MAX_VALUE };

        ArrayNode array = DoubleStream.of(values)
                .mapToObj(obj -> mapper.valueToTree(obj))
                .map(obj -> (DoubleNode) obj).collect(JacksonUtils.asArray());

        DoubleStream numberStream = JacksonUtils.numberStream(array);

        Assertions.assertArrayEquals(values, numberStream.toArray(), 0);
    }

    @Test
    public void testAsArray() {
        Stream<JsonNode> stream = JacksonUtils.stream(createTestArray1());

        ArrayNode array = stream.collect(JacksonUtils.asArray());

        Assertions
                .assertTrue(JacksonUtils.jsonEquals(createTestArray1(), array));
    }

    @Test
    public void testCreateArray() {
        ArrayNode array = JacksonUtils.createArray(mapper.valueToTree("string"),
                mapper.nullNode());

        Assertions.assertEquals(2, array.size());
        Assertions.assertEquals("string", array.get(0).asString());
        Assertions.assertTrue(array.get(1).isNull());
    }

    @Test
    public void testCreateEmptyArray() {
        ArrayNode array = JacksonUtils.createArray();

        Assertions.assertEquals(0, array.size());
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

        Assertions.assertEquals(2, JacksonUtils.getKeys(object).size());
        Assertions.assertEquals(3, object.get("integer").asInt(), 0);
        Assertions.assertEquals("foo", object.get("string").asString());
    }

    @Test
    public void testCreateEmptyObject() {
        ObjectNode object = JacksonUtils.createObject(Collections.emptyMap(),
                item -> {
                    throw new AssertionError("Callback should not be called");
                });

        Assertions.assertEquals(0, JacksonUtils.getKeys(object).size());
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
        Assertions.assertEquals("value", json.get("string").asString());
        Assertions.assertEquals(1.0, json.get("number").asDouble(), 0.0);
        Assertions.assertEquals(2.3f, json.get("flt").floatValue(), 0.0);
        Assertions.assertEquals(4.56, json.get("dbl").asDouble(), 0.0);
    }

    @Test
    public void nestedBeanToJson() {
        ObjectNode json = JacksonUtils.beanToJson(new ParentBean());
        Assertions.assertEquals("parent", json.get("parentValue").asString());
        JsonNode child = json.get("child");
        Assertions.assertEquals("child", child.get("childValue").asString());
    }

    @Test
    public void beanWithTimeFields() {
        BeanWithTemporalFields bean = new BeanWithTemporalFields();
        ObjectNode json = JacksonUtils.beanToJson(bean);

        Assertions.assertTrue(
                JacksonUtils.jsonEquals(JacksonUtils.createNode("10:23:55"),
                        json.get("localTime")),
                "LocalTime not serialized as expected");
        Assertions.assertTrue(
                JacksonUtils.jsonEquals(JacksonUtils.createNode("2024-06-26"),
                        json.get("localDate")),
                "LocalDate not serialized as expected");
        Assertions.assertTrue(
                JacksonUtils.jsonEquals(
                        JacksonUtils.createNode("2024-06-26T10:23:55"),
                        json.get("localDateTime")),
                "LocalDateTime not serialized as expected");
        Assertions.assertEquals(bean.zonedDateTime.toEpochSecond(),
                ZonedDateTime.parse(json.get("zonedDateTime").asString())
                        .toEpochSecond(),
                "ZonedDateTime not serialized as expected");
        Assertions.assertEquals(bean.sqlDate.getTime(),
                ZonedDateTime.parse(json.get("sqlDate").asString()).toInstant()
                        .toEpochMilli(),
                "ZonedDateTime not serialized as expected");
        Assertions.assertEquals(bean.date.getTime(),
                ZonedDateTime.parse(json.get("date").asString()).toInstant()
                        .toEpochMilli(),
                "ZonedDateTime not serialized as expected");
        Assertions.assertEquals(10.0,
                Duration.parse(json.get("duration").asString()).toSeconds(), 0);
    }

    @Test
    public void nullChildBean() {
        ParentBean bean = new ParentBean();
        bean.child = null;

        ObjectNode json = JacksonUtils.beanToJson(bean);
        Assertions.assertEquals(mapper.nullNode(), json.get("child"));
    }

    @Test
    public void beanWithListAndMap() {
        ListAndMapBean bean = new ListAndMapBean();

        ObjectNode json = JacksonUtils.beanToJson(bean);

        JsonNode integerMap = json.get("integerMap");
        Assertions.assertEquals(1, integerMap.get("one").asInt(), 0);
        Assertions.assertEquals(2, integerMap.get("two").asInt(), 0);

        JsonNode childBeanMap = json.get("childBeanMap");
        JsonNode firstChild = childBeanMap.get("First");
        Assertions.assertEquals("firstChildValue",
                firstChild.get("childValue").asString());
        JsonNode secondChild = childBeanMap.get("Second");
        Assertions.assertEquals("secondChildValue",
                secondChild.get("childValue").asString());

        JsonNode integerList = json.get("integerList");
        Assertions.assertEquals(3, integerList.get(0).asInt(), 0);
        Assertions.assertEquals(2, integerList.get(1).asInt(), 0);
        Assertions.assertEquals(1, integerList.get(2).asInt(), 0);

        JsonNode childBeanList = json.get("childBeanList");
        Assertions.assertEquals("firstChildValue",
                childBeanList.get(0).get("childValue").asString());
        Assertions.assertEquals("secondChildValue",
                childBeanList.get(1).get("childValue").asString());
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

        Assertions.assertEquals("bean1", json.get(0).get("string").asString());
        Assertions.assertEquals("bean2", json.get(1).get("string").asString());
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

        Assertions.assertEquals("bean1",
                json.get("one").get("string").asString());
        Assertions.assertEquals("bean2",
                json.get("two").get("string").asString());
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

        Assertions.assertEquals("Foo", person.name);
        Assertions.assertEquals(30.5, person.age, 0.0);
        Assertions.assertTrue(person.canSwim);
    }

    @Test
    public void toFileJson() throws JacksonException {
        ObjectNode json = JacksonUtils.beanToJson(new ParentBean());
        Assertions.assertEquals("""
                {
                  "child": {
                    "childValue": "child"
                  },
                  "parentValue": "parent"
                }""", JacksonUtils.toFileJson(json).replace("\r\n", "\n"));

    }

    @Test
    public void writeValue_nullReturnsNullNode() {
        BaseJsonNode result = JacksonUtils.writeValue(null);
        Assertions.assertTrue(result.isNull(), "Expected NullNode");
        Assertions.assertEquals(mapper.nullNode(), result);
    }

}
