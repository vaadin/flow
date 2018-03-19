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
package com.vaadin.spring.internal;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.StandardMethodMetadata;

import com.vaadin.navigator.ViewDisplay;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.spring.server.SpringUIProvider;
import com.vaadin.ui.Component;

/**
 * Bean post processor that scans for {@link SpringViewDisplay} annotations on
 * UI scoped beans or bean classes and registers
 * {@link SpringViewDisplayRegistrationBean} instances for them for
 * {@link SpringUIProvider}.
 *
 * @author Vaadin Ltd
 */
public class SpringViewDisplayPostProcessor implements BeanPostProcessor,
        ApplicationContextAware, BeanFactoryAware {
    private ApplicationContext applicationContext;
    private ConfigurableListableBeanFactory beanFactory;

    private BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SpringViewDisplayPostProcessor.class);

    @Override
    public Object postProcessAfterInitialization(final Object bean,
            String beanName) throws BeansException {

        final Class<?> clazz = bean.getClass();
        if (!Component.class.isAssignableFrom(clazz)
                && !ViewDisplay.class.isAssignableFrom(clazz)) {
            return bean;
        }

        if (beanFactory != null) {
            try {
                BeanDefinition beanDefinition = beanFactory
                        .getMergedBeanDefinition(beanName);
                // ideally would check beanDefinition.getScope() for UI scope,
                // but scope is not always available

                // look for annotations on factory methods
                if (beanDefinition
                        .getSource() instanceof StandardMethodMetadata) {
                    StandardMethodMetadata metadata = (StandardMethodMetadata) beanDefinition
                            .getSource();
                    Map<String, Object> annotationAttributes = metadata
                            .getAnnotationAttributes(
                                    SpringViewDisplay.class.getName());
                    if (annotationAttributes != null) {
                        registerSpringViewDisplayBean(beanName);
                    }
                }
            } catch (NoSuchBeanDefinitionException e) {
                // ignore for fixing #231
                LOGGER.warn(
                        "No bean definition found for bean [{}] with name [{}] in [{}]",
                        bean, beanName, this);

            }
        }
        // look for annotations on classes
        if (clazz.isAnnotationPresent(SpringViewDisplay.class)) {
            registerSpringViewDisplayBean(clazz);
        }

        return bean;
    }

    /**
     * Create a view display registration bean definition to allow accessing
     * annotated view displays for the current UI scope.
     *
     * @param clazz
     *            bean class having the view display annotation, not null
     */
    protected synchronized void registerSpringViewDisplayBean(Class<?> clazz) {
        BeanDefinitionRegistry registry = null;
        if (applicationContext instanceof BeanDefinitionRegistry) {
            registry = (BeanDefinitionRegistry) applicationContext;
        } else if (applicationContext instanceof ConfigurableApplicationContext) {
            ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext)
                    .getBeanFactory();
            if (beanFactory instanceof BeanDefinitionRegistry) {
                registry = (BeanDefinitionRegistry) beanFactory;
            }
        }
        if (registry == null) {
            throw new BeanDefinitionStoreException(
                    "BeanDefinitionRegistry is not accessible");
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(SpringViewDisplayRegistrationBean.class);

        // information needed to extract the values from the current UI scoped
        // beans
        builder.addPropertyValue("beanClass", clazz);

        builder.setScope(UIScopeImpl.VAADIN_UI_SCOPE_NAME);
        builder.setRole(BeanDefinition.ROLE_SUPPORT);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        String name = getBeanNameGenerator().generateBeanName(beanDefinition,
                registry);
        final boolean alreadyRegistered = applicationContext
                .getBeansOfType(SpringViewDisplayRegistrationBean.class)
                .values().stream()
                .map(SpringViewDisplayRegistrationBean::getBeanClass)
                .anyMatch(clazz::equals);
        if (!alreadyRegistered) {
            registry.registerBeanDefinition(name, beanDefinition);
        }
    }

    /**
     * Create a view display registration bean definition to allow accessing
     * annotated view displays for the current UI scope.
     *
     * @param beanName
     *            name of the bean having the view display annotation, not null
     */
    protected synchronized void registerSpringViewDisplayBean(String beanName) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext;
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(SpringViewDisplayRegistrationBean.class);

        // information needed to extract the values from the current UI scoped
        // beans
        builder.addPropertyValue("beanName", beanName);

        builder.setScope(UIScopeImpl.VAADIN_UI_SCOPE_NAME);
        builder.setRole(BeanDefinition.ROLE_SUPPORT);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        String name = getBeanNameGenerator().generateBeanName(beanDefinition,
                registry);
        final boolean alreadyRegistered = applicationContext
                .getBeansOfType(SpringViewDisplayRegistrationBean.class)
                .values().stream()
                .map(SpringViewDisplayRegistrationBean::getBeanName)
                .anyMatch(beanName::equals);
        if (!alreadyRegistered) {
            registry.registerBeanDefinition(name, beanDefinition);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean,
            String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
        }
    }

    public BeanNameGenerator getBeanNameGenerator() {
        return beanNameGenerator;
    }

    public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
        this.beanNameGenerator = beanNameGenerator;
    }

}
