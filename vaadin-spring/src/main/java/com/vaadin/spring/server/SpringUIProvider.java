/*
 * Copyright 2015-2016 The original authors
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
package com.vaadin.spring.server;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.ViewContainer;
import com.vaadin.spring.internal.UIID;
import com.vaadin.spring.internal.ViewContainerPostProcessor;
import com.vaadin.spring.internal.ViewContainerRegistrationBean;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

/**
 * Vaadin {@link com.vaadin.server.UIProvider} that looks up UI classes from the
 * Spring application context. The UI classes must be annotated with
 * {@link com.vaadin.spring.annotation.SpringUI}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 */
public class SpringUIProvider extends UIProvider {

    private static final long serialVersionUID = 6954428459733726004L;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final VaadinSession vaadinSession;

    /**
     * Temporary cache for webApplicationContext, cleared if the session is
     * serialized.
     */
    private transient WebApplicationContext webApplicationContext = null;
    private final Map<String, Class<? extends UI>> pathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<String, Class<? extends UI>> wildcardPathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();

    public SpringUIProvider(VaadinSession vaadinSession) {
        this.vaadinSession = vaadinSession;

        if (getWebApplicationContext() == null) {
            throw new IllegalStateException(
                    "Spring WebApplicationContext not initialized for UI provider. Use e.g. ContextLoaderListener to initialize it.");
        }
        detectUIs();
        if (pathToUIMap.isEmpty()) {
            logger.warn("Found no Vaadin UIs in the application context");
        }
    }

    @SuppressWarnings("unchecked")
    protected void detectUIs() {
        logger.info("Checking the application context for Vaadin UIs");
        final String[] uiBeanNames = getWebApplicationContext()
                .getBeanNamesForAnnotation(SpringUI.class);
        for (String uiBeanName : uiBeanNames) {
            Class<?> beanType = getWebApplicationContext().getType(uiBeanName);
            if (UI.class.isAssignableFrom(beanType)) {
                logger.info("Found Vaadin UI [{}]",
                        beanType.getCanonicalName());
                final String path;
                String tempPath = deriveMappingForUI(uiBeanName);
                if (tempPath.length() > 0 && !tempPath.startsWith("/")) {
                    path = "/".concat(tempPath);
                } else {
                    // remove terminal slash from mapping
                    path = tempPath.replaceAll("/$", "");
                }
                Class<? extends UI> existingBeanType = getUIByPath(path);
                if (existingBeanType != null) {
                    throw new IllegalStateException(String.format(
                            "[%s] is already mapped to the path [%s]",
                            existingBeanType.getCanonicalName(), path));
                }
                logger.debug("Mapping Vaadin UI [{}] to path [{}]",
                        beanType.getCanonicalName(), path);
                mapPathToUI(path, (Class<? extends UI>) beanType);
            }
        }
    }

    /**
     * Derive the name (path) for a UI based on its annotation parameters.
     *
     * If a path is given as a parameter for the annotation, it is used. An
     * empty string maps to the root context.
     *
     * @param uiBeanName
     *            name of the UI bean
     * @return path to map the UI to
     */
    protected String deriveMappingForUI(String uiBeanName) {
        SpringUI annotation = getWebApplicationContext()
                .findAnnotationOnBean(uiBeanName, SpringUI.class);
        return resolvePropertyPlaceholders(annotation.path());
    }

    @Override
    public Class<? extends UI> getUIClass(
            UIClassSelectionEvent uiClassSelectionEvent) {
        final String path = extractUIPathFromRequest(
                uiClassSelectionEvent.getRequest());
        if (pathToUIMap.containsKey(path)) {
            return pathToUIMap.get(path);
        }

        for (Map.Entry<String, Class<? extends UI>> entry : wildcardPathToUIMap
                .entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String extractUIPathFromRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String path = pathInfo;
            final int indexOfBang = path.indexOf('!');
            if (indexOfBang > -1) {
                path = path.substring(0, indexOfBang);
            }

            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path;
        }
        return "";
    }

    protected WebApplicationContext getWebApplicationContext() {
        if (webApplicationContext == null) {
            webApplicationContext = ((SpringVaadinServletService) vaadinSession
                    .getService()).getWebApplicationContext();
        }

        return webApplicationContext;
    }

    protected void mapPathToUI(String path, Class<? extends UI> uiClass) {
        if (path.endsWith("/*")) {
            wildcardPathToUIMap.put(path.substring(0, path.length() - 2),
                    uiClass);
        } else {
            pathToUIMap.put(path, uiClass);
        }
    }

    protected Class<? extends UI> getUIByPath(String path) {
        return pathToUIMap.get(path);
    }

    @Override
    public UI createInstance(UICreateEvent event) {
        final Class<UIID> key = UIID.class;
        final UIID identifier = new UIID(event);
        CurrentInstance.set(key, identifier);
        try {
            logger.debug(
                    "Creating a new UI bean of class [{}] with identifier [{}]",
                    event.getUIClass().getCanonicalName(), identifier);
            UI ui = getWebApplicationContext().getBean(event.getUIClass());
            configureNavigator(ui);
            return ui;
        } finally {
            CurrentInstance.set(key, null);
        }
    }

    /**
     * Configures a UI to use the navigator found by {@link #getNavigator()} if
     * there is a {@link ViewContainer} annotation.
     *
     * @param ui
     *            the Spring managed UI instance for which to configure
     *            automatic navigation
     */
    protected void configureNavigator(UI ui) {
        Object viewContainer = findViewContainer(ui);
        if (viewContainer == null) {
            return;
        }
        SpringNavigator navigator = getNavigator();
        if (navigator == null) {
            return;
        }

        if (viewContainer instanceof ViewDisplay) {
            navigator.init(ui, (ViewDisplay) viewContainer);
        } else if (viewContainer instanceof SingleComponentContainer) {
            navigator.init(ui, (SingleComponentContainer) viewContainer);
        } else if (viewContainer instanceof ComponentContainer) {
            navigator.init(ui, (ComponentContainer) viewContainer);
        } else {
            logger.error(
                    "View container does not implement ViewDisplay/SingleComponentContainer/ComponentContainer: "
                            + viewContainer);
            throw new IllegalStateException(
                    "View container does not implement ViewDisplay/SingleComponentContainer/ComponentContainer: "
                            + viewContainer);
        }
    }

    /**
     * Returns the configured navigator bean or null if no bean defined.
     *
     * @return bean extending {@link SpringNavigator} or null if none defined
     * @throws BeansException
     *             if there are multiple navigator beans or other configuration
     *             problem
     */
    protected SpringNavigator getNavigator() {
        try {
            return getWebApplicationContext().getBean(SpringNavigator.class);
        } catch (NoUniqueBeanDefinitionException e) {
            throw e;
        } catch (NoSuchBeanDefinitionException e) {
            // While relying on exceptions here is not very nice, using
            // getBean(Class) takes scopes, qualifiers etc. into account
            // consistently.
            // This is somewhat noisy as logged for every UI created.
            logger.info("No Vaadin navigator bean defined");
            return null;
        }
    }

    protected Object findViewContainer(UI ui) {
        try {
            ViewContainerRegistrationBean viewContainerRegistration = getWebApplicationContext()
                    .getBean(ViewContainerRegistrationBean.class);
            return viewContainerRegistration
                    .getViewContainer(getWebApplicationContext());
        } catch (NoUniqueBeanDefinitionException e) {
            throw e;
        } catch (NoSuchBeanDefinitionException e) {
            // fallback with getBeanNamesForAnnotation()
            logger.debug(
                    "Looking for a ViewContainer bean based on bean level annotations");
            final String[] viewContainerBeanNames = getWebApplicationContext()
                    .getBeanNamesForAnnotation(ViewContainer.class);
            if (viewContainerBeanNames.length == 0) {
                logger.debug(
                        "No view container defined for the UI " + ui.getId());
                return null;
            }
            if (viewContainerBeanNames.length > 1) {
                logger.error("Multiple view containers defined for the UI "
                        + ui.getId() + ": "
                        + Arrays.toString(viewContainerBeanNames));
                throw new NoUniqueBeanDefinitionException(Object.class,
                        Arrays.asList(viewContainerBeanNames));
            }
            return getWebApplicationContext()
                    .getBean(viewContainerBeanNames[0]);
        }
    }

    protected ViewContainerPostProcessor getViewContainerPostProcessor() {
        try {
            return getWebApplicationContext()
                    .getBean(ViewContainerPostProcessor.class);
        } catch (NoUniqueBeanDefinitionException e) {
            logger.error(
                    "Multiple " + ViewContainerPostProcessor.class.getName()
                            + " beans exist");
            throw e;
        } catch (NoSuchBeanDefinitionException e) {
            // This is somewhat noisy as potentially logged for every UI
            // created.
            logger.info(ViewContainerPostProcessor.class.getName()
                    + " is not active");
            return null;
        }
    }

    @Override
    public String getTheme(UICreateEvent event) {
        String theme = super.getTheme(event);
        if (theme != null) {
            theme = resolvePropertyPlaceholders(theme);
        }
        return theme;
    }

    @Override
    public String getPageTitle(UICreateEvent event) {
        String pageTitle = super.getPageTitle(event);
        if (pageTitle != null) {
            pageTitle = resolvePropertyPlaceholders(pageTitle);
        }
        return pageTitle;
    }

    @Override
    public String getWidgetset(UICreateEvent event) {
        String widgetset = super.getWidgetset(event);
        if (widgetset != null) {
            widgetset = resolvePropertyPlaceholders(widgetset);
        }
        return widgetset;
    }

    private String resolvePropertyPlaceholders(String value) {
        if (StringUtils.hasText(value)) {
            return webApplicationContext.getEnvironment()
                    .resolvePlaceholders(value);
        }
        return value;
    }

}
