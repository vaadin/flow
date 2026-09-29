/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.List;
import java.util.Set;

import org.atmosphere.cpr.ApplicationConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import com.vaadin.flow.server.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(classes = SpringBootAutoConfiguration.class)
@TestPropertySource(properties = { "vaadin.urlMapping = /zing/*" })
class SpringBootAutoConfigurationUrlMappedTest {

    @Autowired
    private ServletRegistrationBean<SpringServlet> servletRegistrationBean;
    @Autowired
    private Environment environment;

    @Test
    void urlMappingPassedToAtmosphere() {
        assertFalse(RootMappedCondition
                .isRootMapping(RootMappedCondition.getUrlMapping(environment)));
        assertEquals(Set.of("/zing/*"),
                servletRegistrationBean.getUrlMappings());
        assertEquals("/zing/" + Constants.PUSH_MAPPING,
                servletRegistrationBean.getInitParameters()
                        .get(ApplicationConfig.JSR356_MAPPING_PATH));
    }

    @Test
    void excludeUrlsWithNonRootMapping_warningLogged() {
        VaadinConfigurationProperties properties = new VaadinConfigurationProperties();
        properties.setUrlMapping("/zing/*");
        properties.setExcludeUrls(List.of("/zing/sitemap.txt"));
        Logger logger = Mockito.mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactory = Mockito
                .mockStatic(LoggerFactory.class)) {
            loggerFactory
                    .when(() -> LoggerFactory
                            .getLogger(SpringBootAutoConfiguration.class))
                    .thenReturn(logger);

            SpringBootAutoConfiguration.configureServletRegistrationBean(
                    Mockito.mock(ObjectProvider.class), properties,
                    Mockito.mock(SpringServlet.class));
        }

        Mockito.verify(logger).warn(anyString(),
                eq(List.of("/zing/sitemap.txt")), eq("/zing/*"));
    }
}
