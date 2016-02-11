/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server;

import java.io.Serializable;
import java.util.Locale;

import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;

/**
 * Contains helper methods shared by {@link VaadinServlet}.
 *
 */
public class ServletHelper implements Serializable {

    public static final String UPLOAD_URL_PREFIX = "APP/UPLOAD/";
    /**
     * The default SystemMessages (read-only).
     */
    static final SystemMessages DEFAULT_SYSTEM_MESSAGES = new SystemMessages();

    private ServletHelper() {
    }

    private static void verifyUIClass(String className, ClassLoader classLoader)
            throws ServiceException {
        if (className == null) {
            throw new ServiceException(
                    VaadinSession.UI_PARAMETER + " init parameter not defined");
        }

        // Check that the UI layout class can be found
        try {
            Class<?> uiClass = classLoader.loadClass(className);
            if (!UI.class.isAssignableFrom(uiClass)) {
                throw new ServiceException(
                        className + " does not implement UI");
            }
            // Try finding a default constructor, else throw exception
            uiClass.getConstructor();
        } catch (ClassNotFoundException e) {
            throw new ServiceException(className + " could not be loaded", e);
        } catch (SecurityException e) {
            throw new ServiceException(
                    "Could not access " + className + " class", e);
        } catch (NoSuchMethodException e) {
            throw new ServiceException(
                    className + " doesn't have a public no-args constructor",
                    e);
        }
    }

    private static boolean hasPathPrefix(VaadinRequest request, String prefix) {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null) {
            return false;
        }

        return pathInfo.startsWith(prefix) || pathInfo
                .startsWith(new StringBuilder("/").append(prefix).toString());
    }

    private static boolean isPathInfo(VaadinRequest request, String string) {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null) {
            return false;
        }

        return pathInfo.equals(string) || pathInfo
                .equals(new StringBuilder("/").append(string).toString());
    }

    /**
     * Returns whether the given request is a file upload request.
     *
     * @param request
     *            the request to check
     * @return <code>true</code> if it is a file upload request,
     *         <code>false</code> if not
     */
    public static boolean isFileUploadRequest(VaadinRequest request) {
        return hasPathPrefix(request, UPLOAD_URL_PREFIX);
    }

    /**
     * Returns whether the given request is a published file request.
     *
     * @param request
     *            the request to check
     * @return <code>true</code> if it is a published file request,
     *         <code>false</code> if not
     */
    public static boolean isPublishedFileRequest(VaadinRequest request) {
        return hasPathPrefix(request,
                ApplicationConstants.PUBLISHED_FILE_PATH + "/");
    }

    /**
     * Returns whether the given request is a UIDL request.
     *
     * @param request
     *            the request to check
     * @return <code>true</code> if it is a UIDL request, <code>false</code> if
     *         not
     */
    public static boolean isUIDLRequest(VaadinRequest request) {
        return hasPathPrefix(request, ApplicationConstants.UIDL_PATH + '/');
    }

    /**
     * Returns whether the given request is a app request.
     *
     * @param request
     *            the request to check
     * @return <code>true</code> if it is a app request, <code>false</code> if
     *         not
     */
    public static boolean isAppRequest(VaadinRequest request) {
        return hasPathPrefix(request, ApplicationConstants.APP_PATH + '/');
    }

    /**
     * Returns whether the given request is a heart beat request.
     *
     * @param request
     *            the request to check
     * @return <code>true</code> if it is a heart beat request,
     *         <code>false</code> if not
     */
    public static boolean isHeartbeatRequest(VaadinRequest request) {
        return hasPathPrefix(request,
                ApplicationConstants.HEARTBEAT_PATH + '/');
    }

    /**
     * Returns whether the given request is a push request.
     *
     * @param request
     *            the request to check
     * @return <code>true</code> if it is a push request, <code>false</code> if
     *         not
     */
    public static boolean isPushRequest(VaadinRequest request) {
        return isPathInfo(request, ApplicationConstants.PUSH_PATH);
    }

    /**
     * Initializes the default UI provider and optional custom ui providers for
     * the given session.
     *
     * @param session
     *            to add UI providers to
     * @param vaadinService
     *            the vaadin service for the session
     * @throws ServiceException
     *             if no UI providers could be initialized
     */
    public static void initDefaultUIProvider(VaadinSession session,
            VaadinService vaadinService) throws ServiceException {
        String uiProperty = vaadinService.getDeploymentConfiguration()
                .getUIClassName();

        // Add provider for UI parameter first to give it lower priority
        // (providers are FILO)
        if (uiProperty != null) {
            verifyUIClass(uiProperty, vaadinService.getClassLoader());
            session.addUIProvider(new DefaultUIProvider());
        }

        String uiProviderProperty = vaadinService.getDeploymentConfiguration()
                .getUIProviderClassName();
        // Then add custom UI provider if defined
        if (uiProviderProperty != null) {
            UIProvider uiProvider = getUIProvider(uiProviderProperty,
                    vaadinService.getClassLoader());
            session.addUIProvider(uiProvider);
        }
    }

    private static UIProvider getUIProvider(String uiProviderProperty,
            ClassLoader classLoader) throws ServiceException {
        try {
            Class<?> providerClass = classLoader.loadClass(uiProviderProperty);
            Class<? extends UIProvider> subclass = providerClass
                    .asSubclass(UIProvider.class);
            return subclass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ServiceException(
                    "Could not load UIProvider class " + uiProviderProperty, e);
        } catch (ClassCastException e) {
            throw new ServiceException("UIProvider class " + uiProviderProperty
                    + " does not extend UIProvider", e);
        } catch (InstantiationException e) {
            throw new ServiceException(
                    "Could not instantiate UIProvider " + uiProviderProperty,
                    e);
        } catch (IllegalAccessException e) {
            throw new ServiceException(
                    "Could not instantiate UIProvider " + uiProviderProperty,
                    e);
        }
    }

    /**
     * Verifies that there is a UI provider available for the given session.
     *
     * @param session
     *            the session to find UI providers from
     * @throws ServiceException
     */
    public static void checkUiProviders(VaadinSession session)
            throws ServiceException {
        if (session.getUIProviders().isEmpty()) {
            throw new ServiceException(
                    "No UIProvider has been added and there is no \""
                            + VaadinSession.UI_PARAMETER
                            + "\" init parameter.");
        }
    }

    /**
     * Helper to find the most most suitable Locale. These potential sources are
     * checked in order until a Locale is found:
     * <ol>
     * <li>The passed component (or UI) if not null</li>
     * <li>{@link UI#getCurrent()} if defined</li>
     * <li>The passed session if not null</li>
     * <li>{@link VaadinSession#getCurrent()} if defined</li>
     * <li>The passed request if not null</li>
     * <li>{@link VaadinService#getCurrentRequest()} if defined</li>
     * <li>{@link Locale#getDefault()}</li>
     * </ol>
     *
     * @param session
     *            the session that is searched for locale or <code>null</code>
     *            if not available
     * @param request
     *            the request that is searched for locale or <code>null</code>
     *            if not available
     * @return
     */
    public static Locale findLocale(VaadinSession session,
            VaadinRequest request) {

        if (session == null) {
            session = VaadinSession.getCurrent();
        }
        if (session != null) {
            Locale locale = session.getLocale();
            if (locale != null) {
                return locale;
            }
        }

        if (request == null) {
            request = VaadinService.getCurrentRequest();
        }
        if (request != null) {
            Locale locale = request.getLocale();
            if (locale != null) {
                return locale;
            }
        }

        return Locale.getDefault();
    }
}
