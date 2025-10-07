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
package com.vaadin.flow.server.menu;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.vaadin.flow.router.MenuData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class AvailableViewInfoTest {

    private record Badge(String text, String color) {
    }

    private record Detail(String description, int importance, Badge badge) {
    }

    ObjectMapper mapper;
    MenuData menuData;
    Badge badge;
    Detail detail;
    String detailAsString;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        menuData = new MenuData("title", 1.0, false, "icon", null);
        badge = new Badge("New!", "green");
        detail = new Detail("A nice page", 123, badge);
        detailAsString = mapper.writeValueAsString(detail);
    }

    @Test
    public void testEquality() {
        Assert.assertEquals("Two instance created the same way are not equal",
                createInfo(true, true), createInfo(true, true));
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        var info = createInfo(true, true);

        var baos = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(baos);
        oos.writeObject(info);
        oos.close();

        var ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
        var deserializedInfo = (AvailableViewInfo) ois.readObject();

        Assert.assertEquals("Serialized instance is not equal to origin", info,
                deserializedInfo);
    }

    @Test
    public void testJsonSerialization() throws JacksonException {
        var info = createInfo(true, true);
        var json = mapper.writeValueAsString(info);
        Assert.assertEquals("JSON conversion doesn't give the same object",
                info, mapper.readValue(json, AvailableViewInfo.class));
    }

    @Test
    public void testJsonSerializationNull() throws JacksonException {
        var info = createInfo(true, false);
        var json = mapper.writeValueAsString(info);
        Assert.assertEquals("JSON conversion doesn't give the same object",
                info, mapper.readValue(json, AvailableViewInfo.class));
    }

    private AvailableViewInfo createInfo(boolean withChild,
            boolean withDetail) {
        return new AvailableViewInfo("Title", new String[] { "role1" }, false,
                "route", false, true, menuData,
                withChild ? List.of(createInfo(false, withDetail)) : List.of(),
                Map.of("param", RouteParamType.REQUIRED), false,
                withDetail ? detailAsString : null);
    }
}
