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
package com.vaadin.flow.spring;

import java.util.Set;

import org.atmosphere.cpr.ApplicationConfig;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import com.vaadin.flow.server.Constants;

@SpringBootTest(classes = SpringBootAutoConfiguration.class)
@TestPropertySource(properties = { "vaadin.urlMapping = /zing/*" })
public class SpringBootAutoConfigurationUrlMappedTest {

    @Autowired
    private ServletRegistrationBean<SpringServlet> servletRegistrationBean;
    @Autowired
    private Environment environment;

    @Test
    public void urlMappingPassedToAtmosphere() {
        Assert.assertFalse(RootMappedCondition
                .isRootMapping(RootMappedCondition.getUrlMapping(environment)));
        Assert.assertEquals(Set.of("/zing/*"),
                servletRegistrationBean.getUrlMappings());
        Assert.assertEquals("/zing/" + Constants.PUSH_MAPPING,
                servletRegistrationBean.getInitParameters()
                        .get(ApplicationConfig.JSR356_MAPPING_PATH));
    }
}
