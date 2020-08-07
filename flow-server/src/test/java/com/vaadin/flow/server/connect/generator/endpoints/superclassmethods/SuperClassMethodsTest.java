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

import com.vaadin.flow.server.connect.generator.endpoints.AbstractEndpointGenerationTest;
import com.vaadin.flow.server.connect.generator.endpoints.superclassmethods.PersonEndpoint.Person;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import io.swagger.v3.oas.models.OpenAPI;

public class SuperClassMethodsTest extends AbstractEndpointGenerationTest {

  public SuperClassMethodsTest() {
    super(Arrays.asList(PersonEndpoint.class));
  }

  @Test
  public void should_ExportSuperClassMethods() {
    OpenAPI actualOpenAPI = getOpenApiObject();
    Assert.assertEquals(3, actualOpenAPI.getPaths().size());
    Assert.assertEquals(Person.class.getCanonicalName(), extractReturnTypeOfMethod(actualOpenAPI, "get"));
    Assert.assertEquals(Person.class.getCanonicalName(), extractReturnTypeOfMethod(actualOpenAPI, "update"));
  }

  private String extractReturnTypeOfMethod(OpenAPI actualOpenAPI, String methodName) {
    return actualOpenAPI.getPaths().get("/PersonEndpoint/"+methodName).getPost().getResponses().get("200").getContent().get("application/json").getSchema().getName();
  }
}