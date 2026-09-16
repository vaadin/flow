/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.cdi.DeploymentValidator.BeanInfo;
import com.vaadin.cdi.annotation.NormalRouteScoped;
import com.vaadin.cdi.annotation.NormalUIScoped;
import com.vaadin.cdi.annotation.VaadinServiceEnabled;
import com.vaadin.cdi.context.ContextWrapper;
import com.vaadin.cdi.context.RouteScopedContext;
import com.vaadin.cdi.context.UIScopedContext;
import com.vaadin.cdi.context.VaadinServiceScopedContext;
import com.vaadin.cdi.context.VaadinSessionScopedContext;
import com.vaadin.cdi.util.AbstractContext;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.cdi.util.DependentProvider;

/**
 * CDI Extension needed to register Vaadin scopes to the runtime.
 */
public class VaadinExtension implements Extension {

    private VaadinServiceScopedContext serviceScopedContext;
    private VaadinSessionScopedContext sessionScopedContext;
    private UIScopedContext uiScopedContext;
    private RouteScopedContext routeScopedContext;
    private Set<BeanInfo> beanInfoSet = new HashSet<>();

    private void storeBeanValidationInfo(@Observes ProcessBean processBean) {
        beanInfoSet.add(new BeanInfo(processBean.getBean(),
                processBean.getAnnotated()));
    }

    private void addContexts(@Observes AfterBeanDiscovery afterBeanDiscovery,
            BeanManager beanManager) {
        serviceScopedContext = new VaadinServiceScopedContext(beanManager);
        sessionScopedContext = new VaadinSessionScopedContext(beanManager);
        uiScopedContext = new UIScopedContext(beanManager);
        routeScopedContext = new RouteScopedContext(beanManager);
        addContext(afterBeanDiscovery, serviceScopedContext, null);
        addContext(afterBeanDiscovery, sessionScopedContext, null);
        addContext(afterBeanDiscovery, uiScopedContext, NormalUIScoped.class);
        addContext(afterBeanDiscovery, routeScopedContext,
                NormalRouteScoped.class);
    }

    // Validate annotated executor

    private void initializeContexts(@Observes AfterDeploymentValidation adv,
            BeanManager beanManager) {
        serviceScopedContext.init(beanManager);
        sessionScopedContext.init(beanManager);
        uiScopedContext.init(beanManager);
        routeScopedContext.init(beanManager, uiScopedContext::isActive);
    }

    private void validateDeployment(@Observes AfterDeploymentValidation adv,
            BeanManager beanManager) {
        DependentProvider<DeploymentValidator> validatorProvider = BeanProvider
                .getDependent(beanManager, DeploymentValidator.class);
        DeploymentValidator validator = validatorProvider.get();
        validator.validate(beanInfoSet, adv::addDeploymentProblem);
        validatorProvider.destroy();
        beanInfoSet = null;
    }

    private void ensureAtMostOneVaadinTaskExecutor(
            @Observes AfterDeploymentValidation event,
            BeanManager beanManager) {
        Set<Bean<?>> candidates = beanManager.getBeans(Executor.class,
                BeanLookup.SERVICE);
        if (candidates.size() > 1) {
            event.addDeploymentProblem(new IllegalStateException(
                    "There must be at most one Executor bean annotated with @"
                            + VaadinServiceEnabled.class.getSimpleName()
                            + " in the application. " + "Found "
                            + candidates.size() + ": " + candidates));
        }
    }

    private void addContext(AfterBeanDiscovery afterBeanDiscovery,
            AbstractContext context,
            Class<? extends Annotation> additionalScope) {
        afterBeanDiscovery
                .addContext(new ContextWrapper(context, context.getScope()));
        if (additionalScope != null) {
            afterBeanDiscovery
                    .addContext(new ContextWrapper(context, additionalScope));
        }
        getLogger().info("{} registered for Vaadin CDI",
                context.getClass().getSimpleName());
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinExtension.class);
    }

}
