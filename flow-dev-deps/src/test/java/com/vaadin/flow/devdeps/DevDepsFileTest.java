/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.devdeps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonObject;

public class DevDepsFileTest {

    @Test
    public void devDepsFilePresentsAndHasSomeContent() throws IOException {
        InputStream stream = DevDepsFileTest.class
                .getResourceAsStream("/devDependencies.json");
        Assert.assertNotNull(stream);

        JsonObject object = Json
                .parse(IOUtils.toString(stream, StandardCharsets.UTF_8));
        // locked versions has to contain webpack version at least
        Assert.assertTrue("Generated dev deps json doesn't contain webpack",
                object.hasKey("webpack"));
    }
}
