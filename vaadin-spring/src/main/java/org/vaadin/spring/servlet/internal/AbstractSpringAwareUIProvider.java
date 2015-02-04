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
package org.vaadin.spring.servlet.internal;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.vaadin.spring.internal.UIID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for {@link org.vaadin.spring.servlet.SpringAwareUIProvider} and its TouchKit counterpart. Intended only for internal use by the framework.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
public abstract class AbstractSpringAwareUIProvider extends UIProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final long serialVersionUID = -6195911893325385491L;
    private final WebApplicationContext webApplicationContext;
    private final Map<String, Class<? extends UI>> pathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<String, Class<? extends UI>> wildcardPathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();

    public AbstractSpringAwareUIProvider(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
        detectUIs();
        if (pathToUIMap.isEmpty()) {
            logger.warn("Found no Vaadin UIs in the application context");
        }
    }

    protected abstract void detectUIs();

    @Override
    public Class<? extends UI> getUIClass(UIClassSelectionEvent uiClassSelectionEvent) {
        final String path = extractUIPathFromPathInfo(uiClassSelectionEvent.getRequest().getPathInfo());
        if (pathToUIMap.containsKey(path))
            return pathToUIMap.get(path);

        for (Map.Entry<String, Class<? extends UI>> entry : wildcardPathToUIMap.entrySet()) {
            if (path.startsWith(entry.getKey()))
                return entry.getValue();
        }

        return null;
    }

    private String extractUIPathFromPathInfo(String pathInfo) {
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
        return webApplicationContext;
    }

    protected void mapPathToUI(String path, Class<? extends UI> uiClass) {
        if (path.endsWith("/*"))
            wildcardPathToUIMap.put(path.substring(0, path.length() - 2), uiClass);
        else
            pathToUIMap.put(path, uiClass);
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
            logger.debug("Creating a new UI bean of class [{}] with identifier [{}]", event.getUIClass().getCanonicalName(), identifier);
            return webApplicationContext.getBean(event.getUIClass());
        } finally {
            CurrentInstance.set(key, null);
        }
    }
}
