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

import com.vaadin.flow.server.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@SpringBootConfiguration
public class TestServletConfiguration {

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        MyRequestInterceptor myFilter() {
            return new MyRequestInterceptor();
        }

        @Bean
        VaadinRequestInterceptorServiceInitListener vaadinRequestInterceptorServiceInitListener(
                ObjectProvider<VaadinRequestInterceptor> interceptors) {
            return new VaadinRequestInterceptorServiceInitListener(
                    interceptors);
        }
    }

    static class MyRequestInterceptor implements VaadinRequestInterceptor {

        @Override
        public void requestStart(VaadinRequest request,
                VaadinResponse response) {

        }

        @Override
        public void handleException(VaadinRequest request,
                VaadinResponse response, VaadinSession vaadinSession,
                Exception t) {
        }

        @Override
        public void requestEnd(VaadinRequest request, VaadinResponse response,
                VaadinSession session) {
        }
    }
}