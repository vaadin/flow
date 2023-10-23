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
package com.vaadin.flow.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.testbench.unit.mocks.MockWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(SpringBootAutoConfigurationConditionalTest.class)
@WebAppConfiguration
@ContextConfiguration
public class SpringBootAutoConfigurationConditionalTest
        implements TestExecutionListener {

    private static final ThreadLocal<WebApplicationContext> WEB_APPLICATION_CONTEXT = new ThreadLocal<>();

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        WEB_APPLICATION_CONTEXT.set(new MockWebApplicationContext(
                testContext.getApplicationContext(), new MockServletContext()));
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
        WEB_APPLICATION_CONTEXT.remove();
    }

    @Test
    public void customServletRegistrationBean() {
        new ApplicationContextRunner()
                .withBean(WebApplicationContext.class,
                        WEB_APPLICATION_CONTEXT::get)
                .withUserConfiguration(
                        ServletRegistrationBeanConfiguration.MockServletRegistrationBean.class,
                        SpringBootAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).getBean(ServletRegistrationBean.class)
                            .isInstanceOf(
                                    ServletRegistrationBeanConfiguration.MockServletRegistrationBean.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    public static class ServletRegistrationBeanConfiguration {

        @Bean
        @Autowired
        public ServletRegistrationBean<SpringServlet> servletRegistrationBean(
                final WebApplicationContext webApplicationContext) {
            return new MockServletRegistrationBean(webApplicationContext);
        }

        public static class MockServletRegistrationBean
                extends ServletRegistrationBean<SpringServlet> {

            public MockServletRegistrationBean(
                    final WebApplicationContext webApplicationContext) {
                super(new SpringServlet(webApplicationContext, false));
            }

        }

    }

}
