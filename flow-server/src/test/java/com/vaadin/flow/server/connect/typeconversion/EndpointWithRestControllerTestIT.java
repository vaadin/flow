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

package com.vaadin.flow.server.connect.typeconversion;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({VaadinConnectTypeConversionEndpoints.class, MyRestController.class})
public class EndpointWithRestControllerTestIT extends BaseTypeConversionIT{
    @Autowired
    private MockMvc mvc;

    @Test
    //https://github.com/vaadin/flow/issues/8010
    public void shouldNotExposePrivateAndProtectedFields_when_CallingFromRestAPIs()
        throws Exception {
        String result = mvc.perform(get("/api/get")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assert.assertEquals("{\"name\":\"Bond\"}", result);
    }

    @Test
    //https://github.com/vaadin/flow/issues/8034
    public void should_BeAbleToSerializePrivateFieldsOfABean_when_CallingFromConnectEndPoint() {
        try {
            String result = callMethod("getEntityWithPrivateFields").getContentAsString();
            Assert.assertEquals("{\"codeNumber\":\"007\",\"name\":\"Bond\",\"firstName\":\"James\"}", result);
        } catch (Exception e) {
            fail("failed to serialize a bean with private fields");
        }
    }
}