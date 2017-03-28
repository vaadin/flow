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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.Assert;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the views
 * and dependent beans to the visibility of the view, i.e. the scope is
 * activated when a user navigates into a view and destroyed when the user
 * navigates out of the view. Registered by default as the scope
 * {@value #VAADIN_VIEW_SCOPE_NAME}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @see com.vaadin.spring.annotation.ViewScope
 */
public class ViewScopeImpl implements Scope, BeanFactoryPostProcessor {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ViewScopeImpl.class);

    public static final String VAADIN_VIEW_SCOPE_NAME = "vaadin-view";

    private static ViewCacheRetrievalStrategy viewCacheRetrievalStrategy = new BeanFactoryContextViewCacheRetrievalStrategy();

    private ConfigurableListableBeanFactory beanFactory;

    /**
     * Sets the
     * {@link com.vaadin.spring.internal.ViewCacheRetrievalStrategy}
     * to use.
     */
    public static synchronized void setViewCacheRetrievalStrategy(
            ViewCacheRetrievalStrategy viewCacheRetrievalStrategy) {
        if (viewCacheRetrievalStrategy == null) {
            viewCacheRetrievalStrategy = new BeanFactoryContextViewCacheRetrievalStrategy();
        }
        ViewScopeImpl.viewCacheRetrievalStrategy = viewCacheRetrievalStrategy;
    }

    /**
     * Returns the
     * {@link com.vaadin.spring.internal.ViewCacheRetrievalStrategy}
     * to use. By default,
     * {@link com.vaadin.spring.internal.ViewScopeImpl.BeanFactoryContextViewCacheRetrievalStrategy}
     * is used.
     */
    public static synchronized ViewCacheRetrievalStrategy getViewCacheRetrievalStrategy() {
        return viewCacheRetrievalStrategy;
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        return getViewCache().getCurrentViewBeanStore()
                .get(name, objectFactory);
    }

    @Override
    public Object remove(String name) {
        return getViewCache().getCurrentViewBeanStore().remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        getViewCache().getCurrentViewBeanStore().registerDestructionCallback(
                name, callback);
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code null}.
     */
    @Override
    public String getConversationId() {
        return null;
    }

    private ViewCache getViewCache() {
        Assert.notNull(beanFactory, "beanFactory has not been set yet");
        return viewCacheRetrievalStrategy.getViewCache(beanFactory);
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        LOGGER.debug("Registering Vaadin View scope with bean factory [{}]",
                beanFactory);
        beanFactory.registerScope(VAADIN_VIEW_SCOPE_NAME, this);
    }

    /**
     * Implementation of
     * {@link com.vaadin.spring.internal.ViewCacheRetrievalStrategy}
     * that fetches the {@link com.vaadin.spring.internal.ViewCache}
     * instance from the provided bean factory.
     */
    public static class BeanFactoryContextViewCacheRetrievalStrategy implements
    ViewCacheRetrievalStrategy {

        @Override
        public ViewCache getViewCache(BeanFactory beanFactory) {
            return beanFactory.getBean(ViewCache.class);
        }
    }
}
