/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.io.Serializable;
import java.util.Set;

import javax.servlet.annotation.HandlesTypes;

import com.vaadin.base.devserver.startup.DevModeInitializer;
import com.vaadin.base.devserver.startup.DevModeStartupListener;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.VaadinInitializerException;

/**
 * Provides API to access to the {@link DevModeHandler} instance.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class DevModeHandlerManagerImpl implements DevModeHandlerManager {

    /*
     * Attribute key for storing Dev Mode Handler startup flag.
     * 
     * If presented in Servlet Context, shows the Dev Mode Handler already
     * started / become starting. This attribute helps to avoid Dev Mode running
     * twice.
     *
     * Addresses the issue https://github.com/vaadin/spring/issues/502
     */
    private static final class DevModeHandlerAlreadyStartedAttribute
            implements Serializable {
    }

    private DevModeHandler devModeHandler;

    @Override
    public Class<?>[] getHandlesTypes() {
        return DevModeStartupListener.class.getAnnotation(HandlesTypes.class)
                .value();
    }

    @Override
    public void setDevModeHandler(DevModeHandler devModeHandler) {
        if (this.devModeHandler != null) {
            throw new IllegalStateException(
                    "Unable to initialize dev mode handler. A handler is already present: "
                            + this.devModeHandler);
        }
        this.devModeHandler = devModeHandler;
    }

    @Override
    public DevModeHandler getDevModeHandler() {
        return devModeHandler;
    }

    @Override
    public void initDevModeHandler(Set<Class<?>> classes, VaadinContext context)
            throws VaadinInitializerException {
        setDevModeHandler(
                DevModeInitializer.initDevModeHandler(classes, context));
        setDevModeStarted(context);

    }

    private void setDevModeStarted(VaadinContext context) {
        context.setAttribute(DevModeHandlerAlreadyStartedAttribute.class,
                new DevModeHandlerAlreadyStartedAttribute());
    }

    /**
     * Shows whether {@link DevModeHandler} has been already started or not.
     *
     * @param context
     *            The {@link VaadinContext}, not <code>null</code>
     * @return <code>true</code> if {@link DevModeHandler} has already been
     *         started, <code>false</code> - otherwise
     */
    public static boolean isDevModeAlreadyStarted(VaadinContext context) {
        assert context != null;
        return context.getAttribute(
                DevModeHandlerAlreadyStartedAttribute.class) != null;
    }

}
