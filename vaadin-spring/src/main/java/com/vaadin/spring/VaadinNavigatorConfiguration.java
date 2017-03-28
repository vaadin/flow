/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.internal.SpringViewDisplayPostProcessor;
import com.vaadin.spring.navigator.SpringNavigator;

/**
 * Spring configuration for automatically configuring a SpringNavigator.
 *
 * Instead of using this class directly, it is recommended to add the
 * {@link EnableVaadinNavigation} annotation to a configuration class to
 * automatically import {@link VaadinNavigatorConfiguration}.
 *
 * @author Henri Sara (hesara@vaadin.com)
 */
@Configuration
public class VaadinNavigatorConfiguration {

    @Bean
    @UIScope
    public SpringNavigator vaadinNavigator() {
        return new SpringNavigator();
    }

    @Bean
    public static SpringViewDisplayPostProcessor springViewDisplayPostProcessor() {
        return new SpringViewDisplayPostProcessor();
    }

}
