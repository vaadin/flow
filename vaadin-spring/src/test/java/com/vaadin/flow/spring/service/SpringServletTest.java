/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.spring.service;

import jakarta.servlet.ServletException;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "vaadin.push-mode=MANUAL" })
@Import(TestServletConfiguration.class)
public class SpringServletTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void readUniformNameProperty_propertyNameContainsDash_propertyNameIsConvertedToCamelCaseAndReadProperly()
            throws ServletException {
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        PushMode pushMode = service.getDeploymentConfiguration().getPushMode();
        Assert.assertEquals(PushMode.MANUAL, pushMode);
    }

}
