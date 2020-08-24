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
package com.vaadin.flow.server.connect.generator.endpoints.superclassmethods;

import java.util.Arrays;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.server.connect.generator.endpoints.AbstractEndpointGenerationTest;
import com.vaadin.flow.server.connect.generator.endpoints.superclassmethods.PersonEndpoint.Person;

public class SuperClassMethodsTest extends AbstractEndpointGenerationTest {

    public SuperClassMethodsTest() {
        super(Arrays.asList(PersonEndpoint.class));
    }

    @Test
    public void should_ExportSuperClassMethods() {
        OpenAPI actualOpenAPI = getOpenApiObject();
        Assert.assertEquals(3, actualOpenAPI.getPaths().size());
        Assert.assertEquals(Person.class.getCanonicalName(),
                extractReturnTypeOfMethod(actualOpenAPI, "update"));
        Assert.assertEquals(Person.class.getCanonicalName(),
                extractReturnTypeOfMethod(actualOpenAPI, "get"));
    }

    private String unwrapComposedAndExtractName(Schema schema) {
        if (schema instanceof ComposedSchema) {
            return unwrapComposedAndExtractName(
                    ((ComposedSchema) schema).getAllOf().get(0));
        }

        return schema.getName();
    }

    private String extractReturnTypeOfMethod(OpenAPI actualOpenAPI,
            String methodName) {
        return unwrapComposedAndExtractName(actualOpenAPI.getPaths()
                .get("/PersonEndpoint/" + methodName).getPost().getResponses()
                .get("200").getContent().get("application/json").getSchema());
    }
}