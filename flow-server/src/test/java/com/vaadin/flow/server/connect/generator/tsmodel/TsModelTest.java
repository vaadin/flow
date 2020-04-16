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
package com.vaadin.flow.server.connect.generator.tsmodel;

import java.util.Collections;

import org.junit.Test;

import com.vaadin.flow.server.connect.generator.endpoints.AbstractEndpointGeneratorBaseTest;
import com.vaadin.flow.server.connect.generator.tsmodel.TsModelEndpoint.TsModel;

import elemental.json.JsonObject;

import static com.vaadin.flow.server.connect.generator.OpenApiObjectGenerator.EXT_VALID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TsModelTest extends AbstractEndpointGeneratorBaseTest {

    public TsModelTest() {
        super(Collections.singletonList(TsModelEndpoint.class));
    }

    @Test
    public void should_addEntityJavaAnnotations_toOpenApi() {

        generateOpenApi(null);

        JsonObject apiJson = readJsonFile(openApiJsonOutput);

        String modelName = TsModel.class.getName().replace('$', '.');

        JsonObject props = apiJson.getObject("components").getObject("schemas")
                .getObject(modelName).getObject("properties");

        assertFalse(props.getObject("foo").hasKey(EXT_VALID));
        assertEquals("AssertFalse", props.getObject("assertFalse")
                .getArray(EXT_VALID).getString(0));
        assertEquals("AssertTrue",
                props.getObject("assertTrue").getArray(EXT_VALID).getString(0));
        assertEquals("Digits(integer = 5, fraction = 2)",
                props.getObject("digits").getArray(EXT_VALID).getString(0));
        assertEquals("NotEmpty",
                props.getObject("notEmpty").getArray(EXT_VALID).getString(0));
        assertEquals("NotNull",
                props.getObject("notEmpty").getArray(EXT_VALID).getString(1));
    }
}
