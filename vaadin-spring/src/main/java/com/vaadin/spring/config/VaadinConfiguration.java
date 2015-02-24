/*
 * Copyright 2015 The original authors
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
package com.vaadin.spring.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.spring.internal.VaadinSessionScope;
import com.vaadin.spring.internal.VaadinUIScope;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.spring.navigator.internal.DefaultViewCache;
import com.vaadin.spring.navigator.internal.VaadinViewScope;
import com.vaadin.spring.navigator.internal.ViewCache;

/**
 * Spring configuration for registering the custom Vaadin scopes, the
 * {@link SpringViewProvider view provider} and some other stuff.
 *
 * @author Josh Long (josh@joshlong.com)
 * @author Petter Holmström (petter@vaadin.com)
 * @author Gert-Jan Timmer (gjr.timmer@gmail.com)
 * @see com.vaadin.spring.annotation.EnableVaadin
 */
@Configuration
public class VaadinConfiguration implements ApplicationContextAware,
        BeanDefinitionRegistryPostProcessor {

    private ApplicationContext applicationContext;
    private BeanDefinitionRegistry beanDefinitionRegistry;

    @Bean
    static VaadinSessionScope vaadinSessionScope() {
        return new VaadinSessionScope();
    }

    @Bean
    static VaadinUIScope vaadinUIScope() {
        return new VaadinUIScope();
    }

    @Bean
    static VaadinViewScope vaadinViewScope() {
        return new VaadinViewScope();
    }

    @Bean
    SpringViewProvider viewProvider() {
        return new SpringViewProvider(applicationContext,
                beanDefinitionRegistry);
    }

    @Bean
    @com.vaadin.spring.annotation.VaadinUIScope
    ViewCache viewCache() {
        return new DefaultViewCache();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(
            BeanDefinitionRegistry registry) throws BeansException {
        beanDefinitionRegistry = registry;
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // NOP
    }

}
