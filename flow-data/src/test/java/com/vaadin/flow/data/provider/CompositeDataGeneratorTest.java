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
package com.vaadin.flow.data.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class CompositeDataGeneratorTest {

    private static class MockDataGenerator implements DataGenerator<String> {

        private String jsonKey;
        private String jsonValue;
        private List<String> processed = new ArrayList<>();
        private List<String> refreshed = new ArrayList<>();

        public MockDataGenerator(String jsonKey, String jsonValue) {
            this.jsonKey = jsonKey;
            this.jsonValue = jsonValue;
        }

        @Override
        public void generateData(String item, ObjectNode jsonObject) {
            jsonObject.put(jsonKey, jsonValue);
            processed.add(item);
        }

        @Override
        public void destroyData(String item) {
            processed.remove(item);
            refreshed.remove(item);
        }

        @Override
        public void refreshData(String item) {
            refreshed.add(item);
        }

        @Override
        public void destroyAllData() {
            processed.clear();
            refreshed.clear();
        }

        public List<String> getProcessed() {
            return processed;
        }

        public List<String> getRefreshed() {
            return refreshed;
        }

    }

    @Test
    public void generateData_innerGeneratorsAreInvoked() {
        CompositeDataGenerator<String> composite = new CompositeDataGenerator<>();

        MockDataGenerator mock1 = new MockDataGenerator("mock1", "value1");
        MockDataGenerator mock2 = new MockDataGenerator("mock2", "value2");
        MockDataGenerator mock3 = new MockDataGenerator("mock3", "value3");

        composite.addDataGenerator(mock1);
        composite.addDataGenerator(mock2);
        composite.addDataGenerator(mock3);

        ObjectNode json = JacksonUtils.createObjectNode();
        composite.generateData("item1", json);

        Assert.assertEquals("value1", json.get("mock1").asText());
        Assert.assertEquals("value2", json.get("mock2").asText());
        Assert.assertEquals("value3", json.get("mock3").asText());
        Assert.assertThat(mock1.getProcessed(), CoreMatchers.hasItem("item1"));
        Assert.assertThat(mock2.getProcessed(), CoreMatchers.hasItem("item1"));
        Assert.assertThat(mock3.getProcessed(), CoreMatchers.hasItem("item1"));
    }

    @Test
    public void refreshData_innerGeneratorsAreInvoked() {
        CompositeDataGenerator<String> composite = new CompositeDataGenerator<>();

        MockDataGenerator mock1 = new MockDataGenerator("mock1", "value1");
        MockDataGenerator mock2 = new MockDataGenerator("mock2", "value2");
        MockDataGenerator mock3 = new MockDataGenerator("mock3", "value3");

        composite.addDataGenerator(mock1);
        composite.addDataGenerator(mock2);
        composite.addDataGenerator(mock3);

        composite.refreshData("item1");

        Assert.assertThat(mock1.getRefreshed(), CoreMatchers.hasItem("item1"));
        Assert.assertThat(mock2.getRefreshed(), CoreMatchers.hasItem("item1"));
        Assert.assertThat(mock3.getRefreshed(), CoreMatchers.hasItem("item1"));
    }

    @Test
    public void destroyData_innerGeneratorsAreInvoked() {
        CompositeDataGenerator<String> composite = new CompositeDataGenerator<>();

        MockDataGenerator mock1 = new MockDataGenerator("mock1", "value1");
        MockDataGenerator mock2 = new MockDataGenerator("mock2", "value2");
        MockDataGenerator mock3 = new MockDataGenerator("mock3", "value3");

        composite.addDataGenerator(mock1);
        composite.addDataGenerator(mock2);
        composite.addDataGenerator(mock3);

        composite.generateData("item1", JacksonUtils.createObjectNode());
        composite.refreshData("item1");
        composite.generateData("item2", JacksonUtils.createObjectNode());
        composite.refreshData("item2");
        composite.destroyData("item1");

        Assert.assertThat(mock1.getProcessed(),
                CoreMatchers.not(CoreMatchers.hasItem("item1")));
        Assert.assertThat(mock1.getProcessed(), CoreMatchers.hasItem("item2"));
        Assert.assertThat(mock2.getProcessed(),
                CoreMatchers.not(CoreMatchers.hasItem("item1")));
        Assert.assertThat(mock2.getProcessed(), CoreMatchers.hasItem("item2"));
        Assert.assertThat(mock3.getProcessed(),
                CoreMatchers.not(CoreMatchers.hasItem("item1")));
        Assert.assertThat(mock3.getProcessed(), CoreMatchers.hasItem("item2"));
    }

    @Test
    public void destroyAllData_innerGeneratorsAreInvoked() {
        CompositeDataGenerator<String> composite = new CompositeDataGenerator<>();

        MockDataGenerator mock1 = new MockDataGenerator("mock1", "value1");
        MockDataGenerator mock2 = new MockDataGenerator("mock2", "value2");
        MockDataGenerator mock3 = new MockDataGenerator("mock3", "value3");

        composite.addDataGenerator(mock1);
        composite.addDataGenerator(mock2);
        composite.addDataGenerator(mock3);

        composite.generateData("item1", JacksonUtils.createObjectNode());
        composite.refreshData("item1");
        composite.generateData("item2", JacksonUtils.createObjectNode());
        composite.refreshData("item2");
        composite.destroyAllData();

        Assert.assertTrue(mock1.getProcessed().isEmpty());
        Assert.assertTrue(mock2.getProcessed().isEmpty());
        Assert.assertTrue(mock3.getProcessed().isEmpty());
    }

    @Test
    public void dataGeneratorRegistration_remove_dataIsDestroyed() {
        CompositeDataGenerator<String> composite = new CompositeDataGenerator<>();

        MockDataGenerator mock1 = new MockDataGenerator("mock", "value1");
        MockDataGenerator mock2 = new MockDataGenerator("mock", "value1");
        Registration registration1 = composite.addDataGenerator(mock1);
        Registration registration2 = composite.addDataGenerator(mock2);

        composite.generateData("item1", JacksonUtils.createObjectNode());
        Assert.assertThat(mock1.getProcessed(), CoreMatchers.hasItem("item1"));
        Assert.assertThat(mock2.getProcessed(), CoreMatchers.hasItem("item1"));

        registration1.remove();
        Assert.assertThat(mock1.getProcessed(),
                CoreMatchers.not(CoreMatchers.hasItem("item1")));
        Assert.assertThat(mock2.getProcessed(), CoreMatchers.hasItem("item1"));

        registration2.remove();
        Assert.assertThat(mock2.getProcessed(),
                CoreMatchers.not(CoreMatchers.hasItem("item1")));
    }

    @Test
    public void addDataGenerator_orderIsPreserved() {
        CompositeDataGenerator<String> cdg = new CompositeDataGenerator<>();
        DataGenerator<String> dg1 = (String, ObjectNode) -> {
        };
        DataGenerator<String> dg2 = (String, ObjectNode) -> {
        };
        List<DataGenerator<String>> expected = Arrays.asList(dg1, dg2);
        cdg.addDataGenerator(dg1);
        cdg.addDataGenerator(dg2);
        assertEquals(expected, new ArrayList<>(cdg.dataGenerators));
    }
}
