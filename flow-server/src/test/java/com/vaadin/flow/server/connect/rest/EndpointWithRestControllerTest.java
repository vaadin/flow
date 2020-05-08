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

package com.vaadin.flow.server.connect.rest;

import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.vaadin.flow.server.connect.EndpointNameChecker;
import com.vaadin.flow.server.connect.ExplicitNullableTypeChecker;
import com.vaadin.flow.server.connect.VaadinConnectController;
import com.vaadin.flow.server.connect.auth.VaadinConnectAccessChecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
@Import({VaadinConnectEndpoints.class, MyRestController.class})
public class EndpointWithRestControllerTest {

    private MockMvc mockMvcForEndpoint;

    @Autowired
    private MockMvc mockMvcForRest;


    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        mockMvcForEndpoint = MockMvcBuilders.standaloneSetup(new VaadinConnectController(
                null, mock(VaadinConnectAccessChecker.class),
                mock(EndpointNameChecker.class),
                mock(ExplicitNullableTypeChecker.class),
                applicationContext,
                mock(ServletContext.class)))
                .build();
        Assert.assertNotEquals(null, applicationContext);
    }

    @Test
    //https://github.com/vaadin/flow/issues/8010
    public void shouldNotExposePrivateAndProtectedFields_when_CallingFromRestAPIs()
        throws Exception {
        String result = mockMvcForRest.perform(get("/api/get")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals("{\"name\":\"Bond\"}", result);
    }

    @Test
    //https://github.com/vaadin/flow/issues/8034
    public void should_BeAbleToSerializePrivateFieldsOfABean_when_CallingFromConnectEndPoint() {
        try {
            String result = callEndpointMethod("getBeanWithPrivateFields");
            assertEquals("{\"codeNumber\":\"007\",\"name\":\"Bond\",\"firstName\":\"James\"}", result);
        } catch (Exception e) {
            fail("failed to serialize a bean with private fields");
        }
    }

    @Test
    //https://github.com/vaadin/flow/issues/8034
    public void should_BeAbleToSerializeABeanWithZonedDateTimeField() {
        try {
            String result = callEndpointMethod("getBeanWithZonedDateTimeField");
            assertNotNull(result);
            assertNotEquals("", result);
            assertNotEquals("{\"message\":\"Failed to serialize endpoint 'VaadinConnectTypeConversionEndpoints' method 'getBeanWithZonedDateTimeField' response. Double check method's return type or specify a custom mapper bean with qualifier 'vaadinEndpointMapper'\"}", result);
        } catch (Exception e) {
            fail("failed to serialize a bean with ZonedDateTime field");
        }
    }

    @Test
    //https://github.com/vaadin/flow/issues/8067
    public void should_RepsectJacksonAnnotation_when_serializeBean() throws Exception {
        String result = callEndpointMethod("getBeanWithJacksonAnnotation");
        assertEquals("{\"name\":null,\"rating\":2,\"bookId\":null}", result);
    }

    @Test
    /**
     * this requires jackson-datatype-jsr310, which is added as a test scope dependency.
     * jackson-datatype-jsr310 is provided in spring-boot-starter-web, which is part of
     * vaadin-spring-boot-starter
     */
    public void should_serializeLocalTimeInExpectedFormat_when_UsingSpringBoot() throws Exception{
        String result = callEndpointMethod("getLocalTime");
        assertEquals("\"08:00:00\"", result);
    }

    private String callEndpointMethod(String methodName) throws Exception {
        String endpointName = VaadinConnectEndpoints.class.getSimpleName();
        String requestUrl = String.format("/%s/%s", endpointName, methodName);
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post(requestUrl)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        return mockMvcForEndpoint.perform(requestBuilder).andReturn().getResponse().getContentAsString();
    }
}

