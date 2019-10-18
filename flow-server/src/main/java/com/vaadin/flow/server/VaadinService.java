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

package com.vaadin.flow.server;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyTreeCache;
import com.vaadin.flow.component.internal.HtmlImportParser;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.ServletHelper.RequestType;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.HeartbeatHandler;
import com.vaadin.flow.server.communication.PwaHandler;
import com.vaadin.flow.server.communication.SessionRequestHandler;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.communication.UidlRequestHandler;
import com.vaadin.flow.server.communication.WebComponentBootstrapHandler;
import com.vaadin.flow.server.communication.WebComponentProvider;
import com.vaadin.flow.server.startup.BundleFilterFactory;
import com.vaadin.flow.server.startup.FakeBrowser;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.AbstractTheme;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An abstraction of the underlying technology, e.g. servlets, for handling
 * browser requests.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public abstract class VaadinService implements Serializable {

    private static final String SEPARATOR = "\n=================================================================";

    public static final String INVALID_ATMOSPHERE_VERSION_WARNING = SEPARATOR
            + "\nVaadin depends on Atmosphere {} but version {} was found.\n"
            + "This might cause compatibility problems if push is used."
            + SEPARATOR;

    public static final String ATMOSPHERE_MISSING_ERROR = SEPARATOR
            + "\nAtmosphere could not be loaded. When using push with Vaadin, the\n"
            + "Atmosphere framework must be present on the classpath.\n"
            + "If using a dependency management system, please add a dependency\n"
            + "to vaadin-push.\n"
            + "If managing dependencies manually, please make sure Atmosphere\n"
            + Constants.REQUIRED_ATMOSPHERE_RUNTIME_VERSION
            + " is included on the classpath.\n" + "Will fall back to using "
            + PushMode.class.getSimpleName() + "." + PushMode.DISABLED.name()
            + "." + SEPARATOR;

    public static final String CANNOT_ACQUIRE_CLASSLOADER_SEVERE = SEPARATOR
            + "Vaadin was unable to acquire class loader from servlet container\n"
            + "to load your application classes. Setup appropriate security\n"
            + "policy to allow invoking Thread.getContextClassLoader() from\n"
            + "VaadinService if you're not using custom class loader.\n"
            + "NullPointerExceptions will be thrown later." + SEPARATOR;

    /**
     * Attribute name for telling
     * {@link VaadinSession#valueUnbound(javax.servlet.http.HttpSessionBindingEvent)}
     * that it should not close a {@link VaadinSession} even though it gets
     * unbound. If a {@code VaadinSession} has an attribute with this name and
     * the attribute value is {@link Boolean#TRUE}, that session will not be
     * closed when it is unbound from the underlying session.
     */
    // Use the old name.reinitializing value for backwards compatibility
    static final String PRESERVE_UNBOUND_SESSION_ATTRIBUTE = VaadinService.class
            .getName() + ".reinitializing";

    /**
     * @deprecated As of 7.0.
     */
    @Deprecated
    public static final String URL_PARAMETER_RESTART_APPLICATION = "restartApplication";

    /**
     * @deprecated As of 7.0.
     */
    @Deprecated
    public static final String URL_PARAMETER_CLOSE_APPLICATION = "closeApplication";

    private static final String REQUEST_START_TIME_ATTRIBUTE = "requestStartTime";

    /**
     * Should never be used directly, always use
     * {@link #getDeploymentConfiguration()}.
     */
    private final DeploymentConfiguration deploymentConfiguration;

    /*
     * Can't use EventRouter for these listeners since it's not thread safe. One
     * option would be to use an EventRouter instance guarded with a lock, but
     * then we would needlessly hold a "global" lock while invoking potentially
     * slow listener implementations.
     */
    private final Set<ServiceDestroyListener> serviceDestroyListeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private final List<SessionInitListener> sessionInitListeners = new CopyOnWriteArrayList<>();
    private final List<UIInitListener> uiInitListeners = new CopyOnWriteArrayList<>();
    private final List<SessionDestroyListener> sessionDestroyListeners = new CopyOnWriteArrayList<>();

    private SystemMessagesProvider systemMessagesProvider = DefaultSystemMessagesProvider
            .get();

    private ClassLoader classLoader;

    private Iterable<RequestHandler> requestHandlers;

    private Iterable<BootstrapListener> bootstrapListeners;

    private Iterable<DependencyFilter> dependencyFilters;

    private boolean atmosphereAvailable = checkAtmosphereSupport();

    /**
     * Keeps track of whether a warning about missing push support has already
     * been logged. This is used to avoid spamming the log with the same message
     * every time a new UI is bootstrapped.
     */
    private boolean pushWarningEmitted = false;

    /**
     * Set to true when {@link #init()} has been run.
     */
    private boolean initialized = false;

    private Router router;

    private Instantiator instantiator;

    private DependencyTreeCache<String> htmlImportDependencyCache;

    private Registration htmlImportDependencyCacheClearRegistration;

    private VaadinContext vaadinContext;

    /**
     * Creates a new vaadin service based on a deployment configuration.
     *
     * @param deploymentConfiguration
     *            the deployment configuration for the service
     */
    public VaadinService(DeploymentConfiguration deploymentConfiguration) {
        this.deploymentConfiguration = deploymentConfiguration;

        final String classLoaderName = getDeploymentConfiguration()
                .getClassLoaderName();
        if (classLoaderName != null) {
            try {
                final Class<?> classLoaderClass = getClass().getClassLoader()
                        .loadClass(classLoaderName);
                final Constructor<?> c = classLoaderClass
                        .getConstructor(ClassLoader.class);
                setClassLoader((ClassLoader) c.newInstance(
                        new Object[] { getClass().getClassLoader() }));
            } catch (final Exception e) {
                throw new RuntimeException(
                        "Could not find specified class loader: "
                                + classLoaderName,
                        e);
            }
        }

        if (getClassLoader() == null) {
            setDefaultClassLoader();
        }
    }

    /**
     * Creates a service. This method is for use by dependency injection
     * frameworks etc. and must be followed by a call to
     * {@link #setClassLoader(ClassLoader)} or {@link #setDefaultClassLoader()}
     * before use. Furthermore {@link #getDeploymentConfiguration()} and {@link #getContext()} should be
     * overridden (or otherwise intercepted) not to return
     * <code>null</code>.
     */
    protected VaadinService() {
        deploymentConfiguration = null;
        vaadinContext = null;
    }

    /**
     * Initializes this service. The service should be initialized before it is
     * used.
     *
     * @throws ServiceException
     *             if a problem occurs when creating the service
     */
    public void init() throws ServiceException {
        instantiator = createInstantiator();

        // init the router now so that registry will be available for
        // modifications
        router = new Router(getRouteRegistry());

        List<RequestHandler> handlers = createRequestHandlers();

        ServiceInitEvent event = new ServiceInitEvent(this);

        // allow service init listeners and DI to use thread local access to
        // e.g. application scoped route registry
        runWithServiceContext(() -> {
            instantiator.getServiceInitListeners()
                    .forEach(listener -> listener.serviceInit(event));

            event.getAddedRequestHandlers().forEach(handlers::add);

            Collections.reverse(handlers);

            requestHandlers = Collections.unmodifiableCollection(handlers);

            dependencyFilters = Stream
                    .concat(instantiator.getDependencyFilters(
                            event.getAddedDependencyFilters()),
                            new BundleFilterFactory().createFilters(this))
                    .collect(Collectors.toList());
            bootstrapListeners = instantiator
                    .getBootstrapListeners(event.getAddedBootstrapListeners())
                    .collect(Collectors.toList());
        });

        DeploymentConfiguration configuration = getDeploymentConfiguration();
        if (!configuration.isProductionMode()) {
            Logger logger = getLogger();
            logger.debug("The application has the following routes: ");
            getRouteRegistry().getRegisteredRoutes().stream()
                    .map(Object::toString).forEach(logger::debug);

            if (configuration.isCompatibilityMode()) {
                UsageStatistics.markAsUsed("flow/Bower", null);
            } else {
                UsageStatistics.markAsUsed("flow/npm", null);
            }
        }

        htmlImportDependencyCache = new DependencyTreeCache<>(path -> {
            List<String> dependencies = new ArrayList<>();
            WebBrowser browser = FakeBrowser.getEs6();
            HtmlImportParser.parseImports(path,
                    resourcePath -> getResourceAsStream(resourcePath, browser,
                            null),
                    resourcePath -> resolveResource(resourcePath, browser),
                    dependency -> {
                        if (!dependency.startsWith(
                                "frontend://bower_components/polymer/")) {
                            dependencies.add(dependency);
                        }
                    });

            return dependencies;
        });

        /*
         * When all reflection caches are cleared, we also clear the HMTL
         * dependnecy cache so that the reflection caches managed by
         * ComponentMetaData won't keep using previously parsed data.
         */
        htmlImportDependencyCacheClearRegistration = ReflectionCache
                .addClearAllAction(htmlImportDependencyCache::clear);

        initialized = true;
    }

    /**
     * Find a route registry to use for this service.
     *
     * @return the route registry to use, not <code>null</code>
     */
    protected abstract RouteRegistry getRouteRegistry();

    protected abstract PwaRegistry getPwaRegistry();

    /**
     * Returns relative context path for given request.
     * Override this method in subclasses.
     *
     * @param request Request.
     * @return Relative context root path for that request.
     */
    public abstract String getContextRootRelativePath(VaadinRequest request);

    /**
     * Called during initialization to add the request handlers for the service.
     * Note that the returned list will be reversed so the last handler will be
     * called first. This enables overriding this method and using add on the
     * returned list to add a custom request handler which overrides any
     * predefined handler.
     *
     * @return The list of request handlers used by this service.
     * @throws ServiceException
     *             if a problem occurs when creating the request handlers
     */
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> handlers = new ArrayList<>();
        handlers.add(new SessionRequestHandler());
        handlers.add(new HeartbeatHandler());
        handlers.add(new UidlRequestHandler());
        handlers.add(new UnsupportedBrowserHandler());
        handlers.add(new StreamRequestHandler());
        PwaRegistry pwaRegistry = getPwaRegistry();
        if (pwaRegistry != null
                && pwaRegistry.getPwaConfiguration().isEnabled()) {
            handlers.add(new PwaHandler(pwaRegistry));
        }

        if (hasWebComponentConfigurations()) {
            handlers.add(new WebComponentProvider());
            handlers.add(new WebComponentBootstrapHandler());
        }

        return handlers;
    }

    private boolean hasWebComponentConfigurations() {
        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(getContext());
        return registry.hasConfigurations();
    }

    /**
     * Creates an instantiator to use with this service.
     * <p>
     * A custom Vaadin service implementation can override this method to pick
     * an instantiator. The method {@link #loadInstantiators()} is used to find
     * a custom instantiator. If there is no one found then the default is used.
     * You may override this method or {@link #loadInstantiators()} in your
     * custom service.
     *
     * @return an instantiator to use, not <code>null</code>
     * @throws ServiceException
     *             if there are multiple applicable instantiators
     * @see #loadInstantiators()
     * @see Instantiator
     */
    protected Instantiator createInstantiator() throws ServiceException {
        return loadInstantiators().orElseGet(() -> {
            DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                    this);
            defaultInstantiator.init(this);
            return defaultInstantiator;
        });
    }

    /**
     * Loads and initializes instantiators.
     * <p>
     * A custom Vaadin service implementation can override this method to pick
     * an instantiator in some other way instead of the default implementation
     * that uses {@link ServiceLoader}.
     * <p>
     * There may be only one applicable instantiator. Otherwise
     * {@link ServiceException} will be thrown.
     *
     * @return an optional instantator, or an empty optional if no instantiator
     *         found
     * @throws ServiceException
     *             if there are multiple applicable instantiators
     * @see #createInstantiator()
     * @see Instantiator
     */
    protected Optional<Instantiator> loadInstantiators()
            throws ServiceException {
        List<Instantiator> instantiators = StreamSupport
                .stream(ServiceLoader.load(Instantiator.class, getClassLoader())
                        .spliterator(), false)
                .filter(iterator -> iterator.init(this))
                .collect(Collectors.toList());
        if (instantiators.size() > 1) {
            throw new ServiceException(
                    "Cannot init VaadinService because there are multiple eligible instantiator implementations: "
                            + instantiators);
        }
        return instantiators.stream().findFirst();
    }

    /**
     * Gets the instantiator used by this service.
     *
     * @return the used instantiator, or <code>null</code> if this service has
     *         not yet been initialized
     * @see #createInstantiator()
     * @see Instantiator
     */
    public Instantiator getInstantiator() {
        return instantiator;
    }

    /**
     * Gets the class loader to use for loading classes loaded by name, e.g.
     * custom UI classes. This is by default the class loader that was used to
     * load the Servlet class to which this service belongs.
     *
     * @return the class loader to use, or <code>null</code>
     * @see #setClassLoader(ClassLoader)
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the class loader to use for loading classes loaded by name, e.g.
     * custom UI classes. Invokers of this method should be careful to not break
     * any existing class loader hierarchy, e.g. by ensuring that a class loader
     * set for this service delegates to the previously set class loader if the
     * class is not found.
     *
     * @param classLoader
     *            the new class loader to set, not <code>null</code>.
     * @see #getClassLoader()
     */
    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException(
                    "Can not set class loader to null");
        }
        this.classLoader = classLoader;
    }

    /**
     * Returns the MIME type of the specified file, or null if the MIME type is
     * not known. The MIME type is determined by the configuration of the
     * container, and may be specified in a deployment descriptor. Common MIME
     * types are "text/html" and "image/gif".
     *
     * @param resourceName
     *            a String specifying the name of a file
     * @return a String specifying the file's MIME type
     * @see ServletContext#getMimeType(String)
     */
    public abstract String getMimeType(String resourceName);

    /**
     * Gets the deployment configuration. Should be overridden (or otherwise
     * intercepted) if the no-arg constructor is used in order to prevent NPEs.
     *
     * @return the deployment configuration
     */
    public DeploymentConfiguration getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    /**
     * Sets the system messages provider to use for getting system messages to
     * display to users of this service.
     *
     * @param systemMessagesProvider
     *            the system messages provider; <code>null</code> is not
     *            allowed.
     * @see #getSystemMessagesProvider()
     */
    public void setSystemMessagesProvider(
            SystemMessagesProvider systemMessagesProvider) {
        if (systemMessagesProvider == null) {
            throw new IllegalArgumentException(
                    "SystemMessagesProvider can not be null.");
        }
        this.systemMessagesProvider = systemMessagesProvider;
    }

    /**
     * Gets the system messages provider currently defined for this service.
     * <p>
     * By default, the {@link DefaultSystemMessagesProvider} which always
     * provides the built-in default {@link SystemMessages} is used.
     * </p>
     *
     * @return the system messages provider; not <code>null</code>
     * @see #setSystemMessagesProvider(SystemMessagesProvider)
     * @see SystemMessagesProvider
     * @see SystemMessages
     */
    public SystemMessagesProvider getSystemMessagesProvider() {
        return systemMessagesProvider;
    }

    /**
     * Gets the system message to use for a specific locale. This method may
     * also be implemented to use information from current instances of various
     * objects, which means that this method might return different values for
     * the same locale under different circumstances.
     *
     * @param locale
     *            the desired locale for the system messages
     * @param request
     *            the request being processed
     * @return the system messages to use
     */
    public SystemMessages getSystemMessages(Locale locale,
            VaadinRequest request) {
        SystemMessagesInfo systemMessagesInfo = new SystemMessagesInfo(locale,
                request, this);
        return getSystemMessagesProvider()
                .getSystemMessages(systemMessagesInfo);
    }

    /**
     * Adds a listener that gets notified when a new Vaadin service session is
     * initialized for this service.
     * <p>
     * Because of the way different service instances share the same session,
     * the listener is not necessarily notified immediately when the session is
     * created but only when the first request for that session is handled by
     * this service.
     *
     * @param listener
     *            the Vaadin service session initialization listener
     * @return a handle that can be used for removing the listener
     * @see SessionInitListener
     */
    public Registration addSessionInitListener(SessionInitListener listener) {
        sessionInitListeners.add(listener);
        return () -> sessionInitListeners.remove(listener);
    }

    /**
     * Adds a listener that gets notified when a new UI has been initialized.
     *
     * @param listener
     *            the UI initialization listener
     * @return a handle that can be used for removing the listener
     * @see UIInitListener
     */
    public Registration addUIInitListener(UIInitListener listener) {
        uiInitListeners.add(listener);
        return () -> uiInitListeners.remove(listener);
    }

    /**
     * Adds a listener that gets notified when a Vaadin service session that has
     * been initialized for this service is destroyed.
     * <p>
     * The session being destroyed is locked and its UIs have been removed when
     * the listeners are called.
     *
     * @param listener
     *            the vaadin service session destroy listener
     * @return a handle that can be used for removing the listener
     * @see #addSessionInitListener(SessionInitListener)
     */
    public Registration addSessionDestroyListener(
            SessionDestroyListener listener) {
        sessionDestroyListeners.add(listener);
        return () -> sessionDestroyListeners.remove(listener);
    }

    /**
     * Fires the
     * {@link BootstrapListener#modifyBootstrapPage(BootstrapPageResponse)}
     * event to all registered {@link BootstrapListener}. This is called
     * internally when the bootstrap page is created, so listeners can intercept
     * the creation and change the result HTML.
     *
     * @param response
     *            The object containing all relevant info needed by listeners to
     *            change the bootstrap page.
     */
    public void modifyBootstrapPage(BootstrapPageResponse response) {
        bootstrapListeners
                .forEach(listener -> listener.modifyBootstrapPage(response));
    }

    /**
     * Handles destruction of the given session. Internally ensures proper
     * locking is done.
     *
     * @param vaadinSession
     *            The session to destroy
     */
    public void fireSessionDestroy(VaadinSession vaadinSession) {
        final VaadinSession session = vaadinSession;
        session.access(() -> {
            if (session.getState() == VaadinSessionState.CLOSED) {
                return;
            }
            if (session.getState() == VaadinSessionState.OPEN) {
                closeSession(session);
            }
            List<UI> uis = new ArrayList<>(session.getUIs());
            for (final UI ui : uis) {
                ui.accessSynchronously(() -> {
                    /*
                     * close() called here for consistency so that it is always
                     * called before a UI is removed. UI.isClosing() is thus
                     * always true in UI.detach() and associated detach
                     * listeners.
                     */
                    if (!ui.isClosing()) {
                        ui.close();
                    }
                    session.removeUI(ui);
                });
            }
            SessionDestroyEvent event = new SessionDestroyEvent(
                    VaadinService.this, session);
            for (SessionDestroyListener listener : sessionDestroyListeners) {
                try {
                    listener.sessionDestroy(event);
                } catch (Exception e) {
                    /*
                     * for now, use the session error handler; in the future,
                     * could have an API for using some other handler for
                     * session init and destroy listeners
                     */
                    session.getErrorHandler().error(new ErrorEvent(e));
                }
            }

            session.setState(VaadinSessionState.CLOSED);
        });
    }

    /**
     * Attempts to find a Vaadin service session associated with this request.
     * <p>
     * Handles locking of the session internally to avoid creation of duplicate
     * sessions by two threads simultaneously.
     * </p>
     *
     * @param request
     *            the request to get a vaadin service session for.
     * @return the vaadin service session for the request, or <code>null</code>
     *         if no session is found and this is a request for which a new
     *         session shouldn't be created.
     * @throws SessionExpiredException
     *             if the session has already expired
     * @see VaadinSession
     */
    public VaadinSession findVaadinSession(VaadinRequest request)
            throws SessionExpiredException {
        VaadinSession vaadinSession = findOrCreateVaadinSession(request);
        if (vaadinSession == null) {
            return null;
        }

        VaadinSession.setCurrent(vaadinSession);
        request.setAttribute(VaadinSession.class.getName(), vaadinSession);

        return vaadinSession;
    }

    /**
     * Associates the given lock with this service and the given wrapped
     * session. This method should not be called more than once when the lock is
     * initialized for the session.
     *
     * @param wrappedSession
     *            The wrapped session the lock is associated with
     * @param lock
     *            The lock object
     * @see #getSessionLock(WrappedSession)
     */
    private void setSessionLock(WrappedSession wrappedSession, Lock lock) {
        if (wrappedSession == null) {
            throw new IllegalArgumentException(
                    "Can't set a lock for a null session");
        }
        Object currentSessionLock = wrappedSession
                .getAttribute(getLockAttributeName());
        assert (currentSessionLock == null
                || currentSessionLock == lock) : "Changing the lock for a session is not allowed";

        wrappedSession.setAttribute(getLockAttributeName(), lock);
    }

    /**
     * Returns the name used to store the lock in the HTTP session.
     *
     * @return The attribute name for the lock
     */
    private String getLockAttributeName() {
        return getServiceName() + ".lock";
    }

    /**
     * Gets the lock instance used to lock the VaadinSession associated with the
     * given wrapped session.
     * <p>
     * This method uses the wrapped session instead of VaadinSession to be able
     * to lock even before the VaadinSession has been initialized.
     * </p>
     *
     * @param wrappedSession
     *            The wrapped session
     * @return A lock instance used for locking access to the wrapped session
     */
    protected Lock getSessionLock(WrappedSession wrappedSession) {
        Object lock = wrappedSession.getAttribute(getLockAttributeName());

        if (lock instanceof ReentrantLock) {
            return (ReentrantLock) lock;
        }

        if (lock == null) {
            return null;
        }

        throw new RuntimeException(
                "Something else than a ReentrantLock was stored in the "
                        + getLockAttributeName() + " in the session");
    }

    /**
     * Locks the given session for this service instance. Typically you want to
     * call {@link VaadinSession#lock()} instead of this method.
     *
     * @param wrappedSession
     *            The session to lock
     * @throws IllegalStateException
     *             if the session is invalidated before it can be locked
     */
    protected void lockSession(WrappedSession wrappedSession) {
        Lock lock = getSessionLock(wrappedSession);
        if (lock == null) {
            /*
             * No lock found in the session attribute. Ensure only one lock is
             * created and used by everybody by doing double checked locking.
             * Assumes there is a memory barrier for the attribute (i.e. that
             * the CPU flushes its caches and reads the value directly from main
             * memory).
             */
            synchronized (VaadinService.class) {
                lock = getSessionLock(wrappedSession);
                if (lock == null) {
                    lock = new ReentrantLock();
                    setSessionLock(wrappedSession, lock);
                }
            }
        }
        lock.lock();

        try {
            // Someone might have invalidated the session between fetching the
            // lock and acquiring it. Guard for this by calling a method that's
            // specified to throw IllegalStateException if invalidated
            // (#12282)
            wrappedSession.getAttribute(getLockAttributeName());
        } catch (IllegalStateException e) {
            lock.unlock();
            throw e;
        }
    }

    /**
     * Releases the lock for the given session for this service instance.
     * Typically you want to call {@link VaadinSession#unlock()} instead of this
     * method.
     *
     * @param wrappedSession
     *            The session to unlock
     */
    protected void unlockSession(WrappedSession wrappedSession) {
        assert getSessionLock(wrappedSession) != null;
        assert ((ReentrantLock) getSessionLock(wrappedSession))
                .isHeldByCurrentThread() : "Trying to unlock the session but it has not been locked by this thread";
        getSessionLock(wrappedSession).unlock();
    }

    private VaadinSession findOrCreateVaadinSession(VaadinRequest request)
            throws SessionExpiredException {
        boolean requestCanCreateSession = requestCanCreateSession(request);
        WrappedSession wrappedSession = getWrappedSession(request,
                requestCanCreateSession);

        try {
            lockSession(wrappedSession);
        } catch (IllegalStateException e) {
            throw new SessionExpiredException();
        }

        try {
            return doFindOrCreateVaadinSession(request,
                    requestCanCreateSession);
        } finally {
            unlockSession(wrappedSession);
        }

    }

    /**
     * Finds or creates a Vaadin session. Assumes necessary synchronization has
     * been done by the caller to ensure this is not called simultaneously by
     * several threads.
     *
     * @param request
     * @param requestCanCreateSession
     * @return
     * @throws SessionExpiredException
     * @throws ServiceException
     */
    private VaadinSession doFindOrCreateVaadinSession(VaadinRequest request,
            boolean requestCanCreateSession) throws SessionExpiredException {
        assert ((ReentrantLock) getSessionLock(request.getWrappedSession()))
                .isHeldByCurrentThread() : "Session has not been locked by this thread";

        /* Find an existing session for this request. */
        VaadinSession session = getExistingSession(request,
                requestCanCreateSession);

        if (session != null) {
            /*
             * There is an existing session. We can use this as long as the user
             * not specifically requested to close or restart it.
             */

            final boolean restartApplication = hasParameter(request,
                    URL_PARAMETER_RESTART_APPLICATION);
            final boolean closeApplication = hasParameter(request,
                    URL_PARAMETER_CLOSE_APPLICATION);

            if (closeApplication) {
                closeSession(session, request.getWrappedSession(false));
                return null;
            } else if (restartApplication) {
                closeSession(session, request.getWrappedSession(false));
                return createAndRegisterSession(request);
            } else {
                return session;
            }
        }

        // No existing session was found

        if (requestCanCreateSession) {
            /*
             * If the request is such that it should create a new session if one
             * as not found, we do that.
             */
            return createAndRegisterSession(request);
        } else {
            /*
             * The session was not found and a new one should not be created.
             * Assume the session has expired.
             */
            throw new SessionExpiredException();
        }

    }

    private static boolean hasParameter(VaadinRequest request,
            String parameterName) {
        return request.getParameter(parameterName) != null;
    }

    /**
     * Creates and registers a new VaadinSession for this service. Assumes
     * proper locking has been taken care of by the caller.
     *
     * @param request
     *            The request which triggered session creation.
     * @return A new VaadinSession instance
     */
    private VaadinSession createAndRegisterSession(VaadinRequest request) {
        assert ((ReentrantLock) getSessionLock(request.getWrappedSession()))
                .isHeldByCurrentThread() : "Session has not been locked by this thread";

        VaadinSession session = createVaadinSession(request);

        VaadinSession.setCurrent(session);

        storeSession(session, request.getWrappedSession());

        // Initial WebBrowser data comes from the request
        session.getBrowser().updateRequestDetails(request);

        session.setConfiguration(getDeploymentConfiguration());

        // Initial locale comes from the request
        if (getInstantiator().getI18NProvider() != null) {
            setLocale(request, session);
        }

        onVaadinSessionStarted(request, session);

        return session;
    }

    private void setLocale(VaadinRequest request, VaadinSession session) {
        I18NProvider provider = getInstantiator().getI18NProvider();
        List<Locale> providedLocales = provider.getProvidedLocales();
        if (providedLocales.size() == 1) {
            session.setLocale(providedLocales.get(0));
        } else {
            Optional<Locale> foundLocale = LocaleUtil
                    .getExactLocaleMatch(request, providedLocales);

            if (!foundLocale.isPresent()) {
                foundLocale = LocaleUtil.getLocaleMatchByLanguage(request,
                        providedLocales);
            }

            // Set locale by match found in I18N provider, first provided locale
            // or else leave as default locale
            if (foundLocale.isPresent()) {
                session.setLocale(foundLocale.get());
            } else if (!providedLocales.isEmpty()) {
                session.setLocale(providedLocales.get(0));
            }
        }
    }

    /**
     * Creates a new Vaadin session for this service and request.
     *
     * @param request
     *            The request for which to create a VaadinSession
     * @return A new VaadinSession
     */
    protected VaadinSession createVaadinSession(VaadinRequest request) {
        return new VaadinSession(this);
    }

    private void onVaadinSessionStarted(VaadinRequest request,
            VaadinSession session) {
        SessionInitEvent event = new SessionInitEvent(this, session, request);
        for (SessionInitListener listener : sessionInitListeners) {
            try {
                listener.sessionInit(event);
            } catch (Exception e) {
                /*
                 * for now, use the session error handler; in the future, could
                 * have an API for using some other handler for session init and
                 * destroy listeners
                 */
                session.getErrorHandler().error(new ErrorEvent(e));
            }
        }
    }

    private void closeSession(VaadinSession vaadinSession,
            WrappedSession session) {
        if (vaadinSession == null) {
            return;
        }

        if (session != null) {
            removeSession(session);
        }
    }

    protected VaadinSession getExistingSession(VaadinRequest request,
            boolean allowSessionCreation) throws SessionExpiredException {

        final WrappedSession session = getWrappedSession(request,
                allowSessionCreation);

        VaadinSession vaadinSession = loadSession(session);

        if (vaadinSession == null) {
            return null;
        }

        return vaadinSession;
    }

    /**
     * Retrieves the wrapped session for the request.
     *
     * @param request
     *            The request for which to retrieve a session
     * @param requestCanCreateSession
     *            true to create a new session if one currently does not exist
     * @return The retrieved (or created) wrapped session
     * @throws SessionExpiredException
     *             If the request is not associated to a session and new session
     *             creation is not allowed
     */
    private WrappedSession getWrappedSession(VaadinRequest request,
            boolean requestCanCreateSession) throws SessionExpiredException {
        final WrappedSession session = request
                .getWrappedSession(requestCanCreateSession);
        if (session == null) {
            throw new SessionExpiredException();
        }
        return session;
    }

    /**
     * Checks whether it's valid to create a new service session as a result of
     * the given request.
     *
     * @param request
     *            the request
     * @return <code>true</code> if it's valid to create a new service session
     *         for the request; else <code>false</code>
     */
    protected abstract boolean requestCanCreateSession(VaadinRequest request);

    /**
     * Gets the currently used Vaadin service. The current service is
     * automatically defined when processing requests related to the service
     * (see {@link ThreadLocal}) and in {@link VaadinSession#access(Command)}
     * and {@link UI#access(Command)}. In other cases, (e.g. from background
     * threads), the current service is not automatically defined.
     *
     * @return the current Vaadin service instance if available, otherwise
     *         <code>null</code>
     * @see #setCurrentInstances(VaadinRequest, VaadinResponse)
     */
    public static VaadinService getCurrent() {
        return CurrentInstance.get(VaadinService.class);
    }

    /**
     * Sets the this Vaadin service as the current service and also sets the
     * current Vaadin request and Vaadin response. This method is used by the
     * framework to set the current instances when a request related to the
     * service is processed and they are cleared when the request has been
     * processed.
     * <p>
     * The application developer can also use this method to define the current
     * instances outside the normal request handling, e.g. when initiating
     * custom background threads.
     * </p>
     *
     * @param request
     *            the Vaadin request to set as the current request, or
     *            <code>null</code> if no request should be set.
     * @param response
     *            the Vaadin response to set as the current response, or
     *            <code>null</code> if no response should be set.
     * @see #getCurrent()
     * @see #getCurrentRequest()
     * @see #getCurrentResponse()
     */
    public void setCurrentInstances(VaadinRequest request,
            VaadinResponse response) {
        setCurrent(this);
        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinResponse.class, response);
    }

    /**
     * Sets the given Vaadin service as the current service.
     *
     * @param service
     *            the service to set
     */
    public static void setCurrent(VaadinService service) {
        CurrentInstance.set(VaadinService.class, service);
    }

    /**
     * Gets the currently processed Vaadin request. The current request is
     * automatically defined when the request is started. The current request
     * can not be used in e.g. background threads because of the way server
     * implementations reuse request instances.
     *
     * @return the current Vaadin request instance if available, otherwise
     *         <code>null</code>
     * @see #setCurrentInstances(VaadinRequest, VaadinResponse)
     */
    public static VaadinRequest getCurrentRequest() {
        return VaadinRequest.getCurrent();
    }

    /**
     * Gets the currently processed Vaadin response. The current response is
     * automatically defined when the request is started. The current response
     * can not be used in e.g. background threads because of the way server
     * implementations reuse response instances.
     *
     * @return the current Vaadin response instance if available, otherwise
     *         <code>null</code>
     * @see #setCurrentInstances(VaadinRequest, VaadinResponse)
     */
    public static VaadinResponse getCurrentResponse() {
        return VaadinResponse.getCurrent();
    }

    /**
     * Gets a unique name for this service. The name should be unique among
     * different services of the same type but the same for corresponding
     * instances running in different JVMs in a cluster. This is typically based
     * on e.g. the configured servlet's name.
     *
     * @return the unique name of this service instance.
     */
    public abstract String getServiceName();

    /**
     * Finds the {@link UI} that belongs to the provided request. This is
     * generally only supported for UIDL requests as other request types are not
     * related to any particular UI or have the UI information encoded in a
     * non-standard way. The returned UI is also set as the current UI (
     * {@link UI#setCurrent(UI)}).
     *
     * @param request
     *            the request for which a UI is desired
     * @return the UI belonging to the request or null if no UI is found
     */
    public UI findUI(VaadinRequest request) {
        // getForSession asserts that the lock is held
        VaadinSession session = loadSession(request.getWrappedSession());

        // Get UI id from the request
        String uiIdString = request
                .getParameter(ApplicationConstants.UI_ID_PARAMETER);
        UI ui = null;
        if (uiIdString != null && session != null) {
            int uiId = Integer.parseInt(uiIdString);
            ui = session.getUIById(uiId);
        }

        UI.setCurrent(ui);
        return ui;
    }

    /**
     * Discards the current session and creates a new session with the same
     * contents. The purpose of this is to introduce a new session key in order
     * to avoid session fixation attacks.
     * <p>
     * Please note that this method makes certain assumptions about how data is
     * stored in the underlying session and may thus not be compatible with some
     * environments.
     *
     * @param request
     *            The Vaadin request for which the session should be
     *            reinitialized
     */
    public static void reinitializeSession(VaadinRequest request) {
        WrappedSession oldSession = request.getWrappedSession();

        // Stores all attributes (security key, reference to this context
        // instance) so they can be added to the new session
        Set<String> attributeNames = oldSession.getAttributeNames();
        Map<String, Object> attrs = new HashMap<>(attributeNames.size() * 2);
        for (String name : attributeNames) {
            Object value = oldSession.getAttribute(name);
            if (value instanceof VaadinSession) {
                // set flag to avoid cleanup
                VaadinSession serviceSession = (VaadinSession) value;
                serviceSession.setAttribute(PRESERVE_UNBOUND_SESSION_ATTRIBUTE,
                        Boolean.TRUE);
            }
            attrs.put(name, value);
        }

        // Invalidate the current session
        oldSession.invalidate();

        // Create a new session
        WrappedSession newSession = request.getWrappedSession();

        // Restores all attributes (security key, reference to this context
        // instance)
        for (Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            newSession.setAttribute(name, value);

            // Ensure VaadinServiceSession knows where it's stored
            if (value instanceof VaadinSession) {
                VaadinSession serviceSession = (VaadinSession) value;
                VaadinService service = serviceSession.getService();
                // Use the same lock instance in the new session
                service.setSessionLock(newSession,
                        serviceSession.getLockInstance());

                service.storeSession(serviceSession, newSession);
                serviceSession.setAttribute(PRESERVE_UNBOUND_SESSION_ATTRIBUTE,
                        null);
            }
        }

    }

    /**
     * Creates and returns a unique ID for the DIV where the UI is to be
     * rendered.
     *
     * @param session
     *            The service session to which the bootstrapped UI will belong.
     * @param request
     *            The request for which a div id is needed
     * @return the id to use in the DOM
     */
    public abstract String getMainDivId(VaadinSession session,
            VaadinRequest request);

    /**
     * Sets the given session to be closed and all its UI state to be discarded
     * at the end of the current request, or at the end of the next request if
     * there is no ongoing one.
     * <p>
     * After the session has been discarded, any UIs that have been left open
     * will give a Session Expired error and a new session will be created for
     * serving new UIs.
     *
     * @param session
     *            the session to close
     * @see SystemMessages#getSessionExpiredCaption()
     */
    public void closeSession(VaadinSession session) {
        session.close();
    }

    /**
     * Called at the end of a request, after sending the response. Closes
     * inactive UIs in the given session, removes closed UIs from the session,
     * and closes the session if it is itself inactive.
     *
     * @param session
     */
    void cleanupSession(VaadinSession session) {
        if (isSessionActive(session)) {
            closeInactiveUIs(session);
            removeClosedUIs(session);
        } else {
            if (session.getState() == VaadinSessionState.OPEN) {
                closeSession(session);
                if (session.getSession() != null) {
                    getLogger().debug("Closing inactive session {}",
                            session.getSession().getId());
                }
            }
            if (session.getSession() != null) {
                /*
                 * If the VaadinSession has no WrappedSession then it has
                 * already been removed from the HttpSession and we do not have
                 * to do it again
                 */
                removeSession(session.getSession());
            }

            /*
             * The session was destroyed during this request and therefore no
             * destroy event has yet been sent
             */
            fireSessionDestroy(session);
        }
    }

    /**
     * Removes those UIs from the given session for which {@link UI#isClosing()
     * isClosing} yields true.
     *
     * @param session
     */
    private void removeClosedUIs(final VaadinSession session) {
        List<UI> uis = new ArrayList<>(session.getUIs());
        for (final UI ui : uis) {
            if (ui.isClosing()) {
                ui.accessSynchronously(() -> {
                    getLogger().debug("Removing closed UI {}", ui.getUIId());
                    session.removeUI(ui);
                });
            }
        }
    }

    /**
     * Closes those UIs in the given session for which {@link #isUIActive}
     * yields false.
     */
    private void closeInactiveUIs(VaadinSession session) {
        final String sessionId = session.getSession().getId();
        for (final UI ui : session.getUIs()) {
            if (!isUIActive(ui) && !ui.isClosing()) {
                ui.accessSynchronously(() -> {
                    getLogger().debug("Closing inactive UI #{} in session {}",
                            ui.getUIId(), sessionId);
                    ui.close();
                });
            }
        }
    }

    /**
     * Returns the number of seconds that must pass without a valid heartbeat or
     * UIDL request being received from a UI before that UI is removed from its
     * session. This is a lower bound; it might take longer to close an inactive
     * UI. Returns a negative number if heartbeat is disabled and timeout never
     * occurs.
     *
     * @return The heartbeat timeout in seconds or a negative number if timeout
     *         never occurs.
     * @see DeploymentConfiguration#getHeartbeatInterval()
     */
    private int getHeartbeatTimeout() {
        // Permit three missed heartbeats before closing the UI
        return (int) (getDeploymentConfiguration().getHeartbeatInterval()
                * (3.1));
    }

    /**
     * Returns the number of seconds that must pass without a valid UIDL request
     * being received for the given session before the session is closed, even
     * though heartbeat requests are received. This is a lower bound; it might
     * take longer to close an inactive session.
     * <p>
     * Returns a negative number if there is no timeout. In this case heartbeat
     * requests suffice to keep the session alive, but it will still eventually
     * expire in the regular manner if there are no requests at all (see
     * {@link WrappedSession#getMaxInactiveInterval()}).
     *
     * @return The UIDL request timeout in seconds, or a negative number if
     *         timeout never occurs.
     * @see DeploymentConfiguration#isCloseIdleSessions()
     * @see #getHeartbeatTimeout()
     */
    private int getUidlRequestTimeout(VaadinSession session) {
        return getDeploymentConfiguration().isCloseIdleSessions()
                ? session.getSession().getMaxInactiveInterval() : -1;
    }

    /**
     * Returns whether the given UI is active (the client-side actively
     * communicates with the server) or whether it can be removed from the
     * session and eventually collected.
     * <p>
     * A UI is active if and only if its {@link UI#isClosing() isClosing}
     * returns false and {@link #getHeartbeatTimeout() getHeartbeatTimeout} is
     * negative or has not yet expired.
     *
     * @param ui
     *            The UI whose status to check
     * @return true if the UI is active, false if it could be removed.
     */
    public boolean isUIActive(UI ui) {
        if (ui.isClosing()) {
            return false;
        }

        // Check for long running tasks
        Lock lockInstance = ui.getSession().getLockInstance();
        if (lockInstance instanceof ReentrantLock
                && ((ReentrantLock) lockInstance).hasQueuedThreads()) {
            /*
             * Someone is trying to access the session. Leaving all UIs alive
             * for now. A possible kill decision will be made at a later time
             * when the session access has ended.
             */
            return true;
        }

        // Check timeout
        long now = System.currentTimeMillis();
        int timeout = 1000 * getHeartbeatTimeout();
        return timeout < 0 || now
                - ui.getInternals().getLastHeartbeatTimestamp() < timeout;
    }

    /**
     * Returns whether the given session is active or whether it can be closed.
     * <p>
     * A session is active if and only if its {@link VaadinSession#getState()}
     * returns {@link VaadinSessionState#OPEN} and
     * {@link #getUidlRequestTimeout(VaadinSession) getUidlRequestTimeout} is
     * negative or has not yet expired.
     *
     * @param session
     *            The session whose status to check
     * @return true if the session is active, false if it could be closed.
     */
    private boolean isSessionActive(VaadinSession session) {
        if (session.getState() != VaadinSessionState.OPEN
                || session.getSession() == null) {
            return false;
        } else {
            long now = System.currentTimeMillis();
            int timeout = 1000 * getUidlRequestTimeout(session);
            return timeout < 0
                    || now - session.getLastRequestTimestamp() < timeout;
        }
    }

    private static final Logger getLogger() {
        return LoggerFactory.getLogger(VaadinService.class.getName());
    }

    /**
     * Called before the framework starts handling a request.
     *
     * @param request
     *            The request
     * @param response
     *            The response
     */
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        if (!initialized) {
            throw new IllegalStateException(
                    "Can not process requests before init() has been called");
        }
        setCurrentInstances(request, response);
        request.setAttribute(REQUEST_START_TIME_ATTRIBUTE, System.nanoTime());
    }

    /**
     * Called after the framework has handled a request and the response has
     * been written.
     *
     * @param request
     *            The request object
     * @param response
     *            The response object
     * @param session
     *            The session which was used during the request or null if the
     *            request did not use a session
     */
    public void requestEnd(VaadinRequest request, VaadinResponse response,
            VaadinSession session) {
        if (session != null) {
            assert VaadinSession.getCurrent() == session;
            session.lock();
            try {
                cleanupSession(session);
                final long duration = (System.nanoTime() - (Long) request
                        .getAttribute(REQUEST_START_TIME_ATTRIBUTE)) / 1000000;
                session.setLastRequestDuration(duration);
            } finally {
                session.unlock();
            }
        }
        CurrentInstance.clearAll();
    }

    /**
     * Returns the request handlers that are registered with this service. The
     * iteration order of the returned collection is the same as the order in
     * which the request handlers will be invoked when a request is handled.
     *
     * @return a collection of request handlers in the order they are invoked
     * @see #createRequestHandlers()
     */
    public Iterable<RequestHandler> getRequestHandlers() {
        return requestHandlers;
    }

    /**
     * Gets the filters which all resource dependencies are passed through
     * before being sent to the client for loading.
     *
     * @return the dependency filters to pass resources dependencies through
     *         before loading
     */
    public Iterable<DependencyFilter> getDependencyFilters() {
        return dependencyFilters;
    }

    /**
     * Handles the incoming request and writes the response into the response
     * object. Uses {@link #getRequestHandlers()} for handling the request.
     * <p>
     * If a session expiration is detected during request handling then each
     * {@link RequestHandler request handler} has an opportunity to handle the
     * expiration event if it implements {@link SessionExpiredHandler}. If no
     * request handler handles session expiration a default expiration message
     * will be written.
     * </p>
     *
     * @param request
     *            The incoming request
     * @param response
     *            The outgoing response
     * @throws ServiceException
     *             Any exception that occurs during response handling will be
     *             wrapped in a ServiceException
     */
    public void handleRequest(VaadinRequest request, VaadinResponse response)
            throws ServiceException {
        requestStart(request, response);

        VaadinSession vaadinSession = null;
        try {
            // Find out the service session this request is related to
            vaadinSession = findVaadinSession(request);
            if (vaadinSession == null) {
                return;
            }

            for (RequestHandler handler : getRequestHandlers()) {
                if (handler.handleRequest(vaadinSession, request, response)) {
                    return;
                }
            }

            // Request not handled by any RequestHandler
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Request was not handled by any registered handler.");

        } catch (final SessionExpiredException e) {
            handleSessionExpired(request, response);
        } catch (final Exception e) {
            handleExceptionDuringRequest(request, response, vaadinSession, e);
        } finally {
            requestEnd(request, response, vaadinSession);
        }
    }

    private void handleExceptionDuringRequest(VaadinRequest request,
            VaadinResponse response, VaadinSession vaadinSession, Exception t)
            throws ServiceException {
        if (vaadinSession != null) {
            vaadinSession.lock();
        }
        try {
            if (vaadinSession != null) {
                vaadinSession.getErrorHandler().error(new ErrorEvent(t));
            }
            // if this was an UIDL request, send UIDL back to the client
            if (ServletHelper.isRequestType(request, RequestType.UIDL)) {
                SystemMessages ci = getSystemMessages(
                        ServletHelper.findLocale(vaadinSession, request),
                        request);
                try {
                    writeUncachedStringResponse(response,
                            JsonConstants.JSON_CONTENT_TYPE,
                            createCriticalNotificationJSON(
                                    ci.getInternalErrorCaption(),
                                    ci.getInternalErrorMessage(), null,
                                    ci.getInternalErrorURL()));
                } catch (IOException e) {
                    // An exception occurred while writing the response. Log
                    // it and continue handling only the original error.
                    getLogger().warn(
                            "Failed to write critical notification response to the client",
                            e);
                }
            } else {
                // Re-throw other exceptions
                throw new ServiceException(t);
            }
        } finally {
            if (vaadinSession != null) {
                vaadinSession.unlock();
            }
        }

    }

    /**
     * Writes the given string as a response using the given content type.
     *
     * @param response
     *            The response reference
     * @param contentType
     *            The content type of the response
     * @param responseString
     *            The actual response
     * @throws IOException
     *             If an error occurred while writing the response
     */
    public void writeStringResponse(VaadinResponse response, String contentType,
            String responseString) throws IOException {

        response.setContentType(contentType);

        final OutputStream out = response.getOutputStream();
        final PrintWriter outWriter = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(out, UTF_8)));
        outWriter.print(responseString);
        outWriter.close();
    }

    /**
     * Writes the given string as a response with headers to prevent caching and
     * using the given content type.
     *
     * @param response
     *            The response reference
     * @param contentType
     *            The content type of the response
     * @param responseString
     *            The actual response
     * @throws IOException
     *             If an error occurred while writing the response
     */
    public void writeUncachedStringResponse(VaadinResponse response,
            String contentType, String responseString) throws IOException {
        // Response might contain sensitive information, so prevent all forms of
        // caching
        response.setNoCacheHeaders();

        writeStringResponse(response, contentType, responseString);
    }

    /**
     * Called when the session has expired and the request handling is therefore
     * aborted.
     *
     * @param request
     *            The request
     * @param response
     *            The response
     * @throws ServiceException
     *             Thrown if there was any problem handling the expiration of
     *             the session
     */
    protected void handleSessionExpired(VaadinRequest request,
            VaadinResponse response) throws ServiceException {
        for (RequestHandler handler : getRequestHandlers()) {
            if (handler instanceof SessionExpiredHandler) {
                try {
                    if (((SessionExpiredHandler) handler)
                            .handleSessionExpired(request, response)) {
                        return;
                    }
                } catch (IOException e) {
                    throw new ServiceException(
                            "Handling of session expired failed", e);
                }
            }
        }

        // No request handlers handled the request. Write a normal HTTP response

        try {
            // If there is a URL, try to redirect there
            SystemMessages systemMessages = getSystemMessages(
                    ServletHelper.findLocale(null, request), request);
            String sessionExpiredURL = systemMessages.getSessionExpiredURL();
            if (sessionExpiredURL != null
                    && (response instanceof VaadinServletResponse)) {
                ((VaadinServletResponse) response)
                        .sendRedirect(sessionExpiredURL);
            } else {
                /*
                 * Session expired as a result of a standard http request and we
                 * have nowhere to redirect. Reloading would likely cause an
                 * endless loop. This can at least happen if refreshing a
                 * resource when the session has expired.
                 */
                response.sendError(HttpServletResponse.SC_GONE,
                        "Session expired");
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Creates a JSON message which, when sent to client as-is, will cause a
     * critical error to be shown with the given details.
     *
     * @param caption
     *            The caption of the error or null to omit
     * @param message
     *            The error message or null to omit
     * @param details
     *            Additional error details or null to omit
     * @param url
     *            A url to redirect to. If no other details are given then the
     *            user will be immediately redirected to this URL. Otherwise the
     *            message will be shown and the browser will redirect to the
     *            given URL only after the user acknowledges the message. If
     *            null then the browser will refresh the current page.
     * @return A JSON string to be sent to the client
     */
    public static String createCriticalNotificationJSON(String caption,
            String message, String details, String url) {
        try {
            JsonObject appError = Json.createObject();
            putValueOrJsonNull(appError, "caption", caption);
            putValueOrJsonNull(appError, "url", url);
            putValueOrJsonNull(appError, "message", message);
            putValueOrJsonNull(appError, "details", details);

            JsonObject meta = Json.createObject();
            meta.put("appError", appError);

            JsonObject json = Json.createObject();
            json.put("changes", Json.createObject());
            json.put("resources", Json.createObject());
            json.put("locales", Json.createObject());
            json.put("meta", meta);
            json.put(ApplicationConstants.SERVER_SYNC_ID, -1);
            return wrapJsonForClient(json);
        } catch (JsonException e) {
            getLogger().warn(
                    "Error creating critical notification JSON message", e);
            return wrapJsonForClient(Json.createObject());
        }

    }

    private static String wrapJsonForClient(JsonObject json) {
        return "for(;;);[" + JsonUtil.stringify(json) + "]";
    }

    /**
     * Creates the JSON to send to the client when the session has expired.
     *
     * @return the JSON used to inform the client about a session expiration, as
     *         a string
     */
    public static String createSessionExpiredJSON() {
        JsonObject json = Json.createObject();
        JsonObject meta = Json.createObject();
        json.put("meta", meta);

        meta.put(JsonConstants.META_SESSION_EXPIRED, true);
        return wrapJsonForClient(json);
    }

    /**
     * Creates the JSON to send to the client when the UI cannot be found.
     *
     * @return the JSON used to inform the client that the UI cannot be found,
     *         as a string
     */
    public static String createUINotFoundJSON() {
        // Session Expired is technically not really the correct thing as
        // the session exists but the requested UI does not. Still we want
        // to handle it the same way on the client side.
        return createSessionExpiredJSON();
    }

    private static void putValueOrJsonNull(JsonObject json, String key,
            String value) {
        if (value == null) {
            json.put(key, Json.createNull());
        } else {
            json.put(key, value);
        }
    }

    /**
     * Enables push if push support is available and push has not yet been
     * enabled.
     *
     * If push support is not available, a warning explaining the situation will
     * be logged at least the first time this method is invoked.
     *
     * @return <code>true</code> if push can be used; <code>false</code> if push
     *         is not available.
     */
    public boolean ensurePushAvailable() {
        if (atmosphereAvailable) {
            return true;
        } else {
            if (!pushWarningEmitted) {
                pushWarningEmitted = true;
                getLogger().warn(ATMOSPHERE_MISSING_ERROR);
            }
            return false;
        }
    }

    private static boolean checkAtmosphereSupport() {
        String rawVersion = AtmospherePushConnection.getAtmosphereVersion();
        if (rawVersion == null) {
            return false;
        }

        if (!Constants.REQUIRED_ATMOSPHERE_RUNTIME_VERSION.equals(rawVersion)) {
            getLogger().warn(INVALID_ATMOSPHERE_VERSION_WARNING,
                    new Object[] {
                            Constants.REQUIRED_ATMOSPHERE_RUNTIME_VERSION,
                            rawVersion });
        }
        return true;
    }

    /**
     * Checks whether Atmosphere is available for use.
     *
     * @return true if Atmosphere is available, false otherwise
     */
    protected boolean isAtmosphereAvailable() {
        return atmosphereAvailable;
    }

    /**
     * Checks that another {@link VaadinSession} instance is not locked. This is
     * internally used by {@link VaadinSession#accessSynchronously(Command)} and
     * {@link UI#accessSynchronously(Command)} to help avoid causing deadlocks.
     *
     * @param session
     *            the session that is being locked
     * @throws IllegalStateException
     *             if the current thread holds the lock for another session
     */
    public static void verifyNoOtherSessionLocked(VaadinSession session) {
        if (isOtherSessionLocked(session)) {
            throw new IllegalStateException(
                    "Can't access session while another session is locked by the same thread. This restriction is intended to help avoid deadlocks.");
        }
    }

    /**
     * Checks whether there might be some {@link VaadinSession} other than the
     * provided one for which the current thread holds a lock. This method might
     * not detect all cases where some other session is locked, but it should
     * cover the most typical situations.
     *
     * @param session
     *            the session that is expected to be locked
     * @return <code>true</code> if another session is also locked by the
     *         current thread; <code>false</code> if no such session was found
     */
    public static boolean isOtherSessionLocked(VaadinSession session) {
        VaadinSession otherSession = VaadinSession.getCurrent();
        if (otherSession == null || otherSession == session) {
            return false;
        }
        return otherSession.hasLock();
    }

    /**
     * Verifies that the given CSRF token (aka double submit cookie) is valid
     * for the given UI. This is used to protect against Cross Site Request
     * Forgery attacks.
     * <p>
     * This protection is enabled by default, but it might need to be disabled
     * to allow a certain type of testing. For these cases, the check can be
     * disabled by setting the init parameter
     * <code>disable-xsrf-protection</code> to <code>true</code>.
     *
     * @param ui
     *            the UI for which the check should be done
     * @param requestToken
     *            the CSRF token provided in the request
     * @return <code>true</code> if the token is valid or if the protection is
     *         disabled; <code>false</code> if protection is enabled and the
     *         token is invalid
     * @see DeploymentConfiguration#isXsrfProtectionEnabled()
     */
    public static boolean isCsrfTokenValid(UI ui, String requestToken) {

        if (ui.getSession().getService().getDeploymentConfiguration()
                .isXsrfProtectionEnabled()) {
            String uiToken = ui.getCsrfToken();

            if (uiToken == null || !uiToken.equals(requestToken)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Implementation for {@link VaadinSession#access(Command)}. This method is
     * implemented here instead of in {@link VaadinSession} to enable overriding
     * the implementation without using a custom subclass of VaadinSession.
     *
     * @param session
     *            the vaadin session to access
     * @param command
     *            the command to run with the session locked
     * @return a future that can be used to check for task completion and to
     *         cancel the task
     * @see VaadinSession#access(Command)
     */
    public Future<Void> accessSession(VaadinSession session, Command command) {
        FutureAccess future = new FutureAccess(session, command);
        session.getPendingAccessQueue().add(future);

        ensureAccessQueuePurged(session);

        return future;
    }

    /**
     * Makes sure the pending access queue is purged for the provided session.
     * If the session is currently locked by the current thread or some other
     * thread, the queue will be purged when the session is unlocked. If the
     * lock is not held by any thread, it is acquired and the queue is purged
     * right away.
     *
     * @param session
     *            the session for which the access queue should be purged
     */
    public void ensureAccessQueuePurged(VaadinSession session) {
        /*
         * If no thread is currently holding the lock, pending changes for UIs
         * with automatic push would not be processed and pushed until the next
         * time there is a request or someone does an explicit push call.
         *
         * To remedy this, we try to get the lock at this point. If the lock is
         * currently held by another thread, we just back out as the queue will
         * get purged once it is released. If the lock is held by the current
         * thread, we just release it knowing that the queue gets purged once
         * the lock is ultimately released. If the lock is not held by any
         * thread and we acquire it, we just release it again to purge the queue
         * right away.
         */
        try {
            // tryLock() would be shorter, but it does not guarantee fairness
            if (session.getLockInstance().tryLock(0, TimeUnit.SECONDS)) {
                // unlock triggers runPendingAccessTasks
                session.unlock();
            }
        } catch (InterruptedException e) {
            // Just ignore
        }
    }

    /**
     * Purges the queue of pending access invocations enqueued with
     * {@link VaadinSession#access(Command)}.
     * <p>
     * This method is automatically run by the framework at appropriate
     * situations and is not intended to be used by application developers.
     *
     * @param session
     *            the vaadin session to purge the queue for
     */
    public void runPendingAccessTasks(VaadinSession session) {
        session.checkHasLock();

        if (session.getPendingAccessQueue().isEmpty()) {
            return;
        }

        FutureAccess pendingAccess;

        // Dump all current instances, not only the ones dumped by setCurrent
        Map<Class<?>, CurrentInstance> oldInstances = CurrentInstance
                .getInstances();
        CurrentInstance.setCurrent(session);
        try {
            while ((pendingAccess = session.getPendingAccessQueue()
                    .poll()) != null) {
                if (!pendingAccess.isCancelled()) {
                    pendingAccess.run();

                    try {
                        pendingAccess.get();

                    } catch (Exception exception) {
                        pendingAccess.handleError(exception);
                    }
                }
            }
        } finally {
            CurrentInstance.clearAll();
            CurrentInstance.restoreInstances(oldInstances);
        }
    }

    /**
     * Adds a service destroy listener that gets notified when this service is
     * destroyed.
     * <p>
     * The listeners may be invoked in a non-deterministic order. In particular,
     * it is not guaranteed that listeners will be invoked in the order they
     * were added.
     *
     * @param listener
     *            the service destroy listener to add
     * @return a handle that can be used for removing the listener
     * @see #destroy()
     * @see ServiceDestroyListener
     */
    public Registration addServiceDestroyListener(
            ServiceDestroyListener listener) {
        serviceDestroyListeners.add(listener);
        return () -> serviceDestroyListeners.remove(listener);
    }

    /**
     * Called when the servlet or similar for this service is being destroyed.
     * After this method has been called, no more requests will be handled by
     * this service.
     *
     * @see #addServiceDestroyListener(ServiceDestroyListener)
     * @see Servlet#destroy()
     */
    public void destroy() {
        htmlImportDependencyCacheClearRegistration.remove();

        ServiceDestroyEvent event = new ServiceDestroyEvent(this);
        serviceDestroyListeners
                .forEach(listener -> listener.serviceDestroy(event));
    }

    /**
     * Tries to acquire default class loader and sets it as a class loader for
     * this {@link VaadinService} if found. If current security policy disallows
     * acquiring class loader instance it will log a message and re-throw
     * {@link SecurityException}
     *
     * @throws SecurityException
     *             If current security policy forbids acquiring class loader
     */
    protected void setDefaultClassLoader() {
        try {
            setClassLoader(
                    VaadinServiceClassLoaderUtil.findDefaultClassLoader());
        } catch (SecurityException e) {
            getLogger().error(CANNOT_ACQUIRE_CLASSLOADER_SEVERE, e);
            throw e;
        }
    }

    /**
     * Called when the VaadinSession should be stored.
     * <p>
     * By default stores the VaadinSession in the underlying HTTP session.
     *
     * @param session
     *            the VaadinSession to store
     * @param wrappedSession
     *            the underlying HTTP session
     */
    protected void storeSession(VaadinSession session,
            WrappedSession wrappedSession) {
        assert VaadinSession.hasLock(this, wrappedSession);
        writeToHttpSession(wrappedSession, session);
        session.refreshTransients(wrappedSession, this);
    }

    /**
     * Performs the actual write of the VaadinSession to the underlying HTTP
     * session after sanity checks have been performed.
     * <p>
     * Called by {@link #storeSession(VaadinSession, WrappedSession)}
     *
     * @param wrappedSession
     *            the underlying HTTP session
     * @param session
     *            the VaadinSession to store
     */
    protected void writeToHttpSession(WrappedSession wrappedSession,
            VaadinSession session) {
        wrappedSession.setAttribute(getSessionAttributeName(), session);
    }

    /**
     * Called when the VaadinSession should be loaded from the underlying HTTP
     * session.
     *
     * @param wrappedSession
     *            the underlying HTTP session
     * @return the VaadinSession in the HTTP session or null if not found
     */
    protected VaadinSession loadSession(WrappedSession wrappedSession) {
        assert VaadinSession.hasLock(this, wrappedSession);

        VaadinSession vaadinSession = readFromHttpSession(wrappedSession);
        if (vaadinSession == null) {
            return null;
        }
        vaadinSession.refreshTransients(wrappedSession, this);
        return vaadinSession;
    }

    /**
     * Performs the actual read of the VaadinSession from the underlying HTTP
     * session after sanity checks have been performed.
     * <p>
     * Called by {@link #loadSession(WrappedSession)}.
     *
     * @param wrappedSession
     *            the underlying HTTP session
     * @return the VaadinSession or null if no session was found
     */
    protected VaadinSession readFromHttpSession(WrappedSession wrappedSession) {
        return (VaadinSession) wrappedSession
                .getAttribute(getSessionAttributeName());
    }

    /**
     * Called when the VaadinSession should be removed from the underlying HTTP
     * session.
     *
     * @param wrappedSession
     *            the underlying HTTP session
     */
    public void removeSession(WrappedSession wrappedSession) {
        assert VaadinSession.hasLock(this, wrappedSession);
        removeFromHttpSession(wrappedSession);
    }

    /**
     * Performs the actual removal of the VaadinSession from the underlying HTTP
     * session after sanity checks have been performed.
     *
     * @param wrappedSession
     *            the underlying HTTP session
     */
    protected void removeFromHttpSession(WrappedSession wrappedSession) {
        wrappedSession.removeAttribute(getSessionAttributeName());

    }

    /**
     * Returns the name used for storing the VaadinSession in the underlying
     * HTTP session.
     *
     * @return the attribute name used for storing the VaadinSession
     */
    protected String getSessionAttributeName() {
        return VaadinSession.class.getName() + "." + getServiceName();
    }

    /**
     * Gets the router used for UIs served by this service.
     *
     * @return the router, not <code>null</code>
     */
    public Router getRouter() {
        return router;
    }

    /**
     * Fire UI initialization event to all registered {@link UIInitListener}s.
     *
     * @param ui
     *            the initialized {@link UI}
     */
    public void fireUIInitListeners(UI ui) {
        UIInitEvent initEvent = new UIInitEvent(ui, this);
        uiInitListeners.forEach(listener -> listener.uiInit(initEvent));
    }

    /**
     * Returns a URL to the static resource at the given URI or null if no file
     * found.
     *
     * @param url
     *            the URL for the resource
     * @return the resource located at the named path, or <code>null</code> if
     *         there is no resource at that path
     */
    public abstract URL getStaticResource(String url);

    /**
     * Returns a URL to the resource at the given Vaadin URI.
     *
     * @param url
     *            the untranslated Vaadin URL for the resource
     * @param browser
     *            the web browser to resolve for, relevant for es5 vs es6
     *            resolving
     * @param theme
     *            the theme to use for translating the URL or <code>null</code>
     *            if no theme is used
     * @return the resource located at the named path, or <code>null</code> if
     *         there is no resource at that path
     */
    public abstract URL getResource(String url, WebBrowser browser,
            AbstractTheme theme);

    /**
     * Opens a stream to to the resource at the given Vaadin URI.
     *
     * @param url
     *            the untranslated Vaadin URL for the resource
     * @param browser
     *            the web browser to resolve for, relevant for es5 vs es6
     *            resolving
     * @param theme
     *            the theme to use for translating the URL or <code>null</code>
     *            if no theme is used
     * @return a stream for the resource or <code>null</code> if no resource
     *         exists at the specified path
     */
    public abstract InputStream getResourceAsStream(String url,
            WebBrowser browser, AbstractTheme theme);

    /**
     * Checks if a resource is available at the given Vaadin URI.
     *
     * @param url
     *            the untranslated Vaadin URL for the resource
     * @param browser
     *            the web browser to resolve for, relevant for es5 vs es6
     *            resolving
     * @param theme
     *            the theme to use for translating the URL or <code>null</code>
     *            if no theme is used
     * @return <code>true</code> if a resource is found and can be read using
     *         {@link #getResourceAsStream(String, WebBrowser, AbstractTheme)},
     *         <code>false</code> if it is not found
     */
    public boolean isResourceAvailable(String url, WebBrowser browser,
            AbstractTheme theme) {
        return getResource(url, browser, theme) != null;
    }

    /**
     * Resolves the given {@code url} resource to be useful for
     * {@link #getResource(String, WebBrowser, AbstractTheme)} and
     * {@link #getResourceAsStream(String, WebBrowser, AbstractTheme)}.
     *
     * @param url
     *            the resource to resolve, not <code>null</code>
     * @param browser
     *            the web browser to resolve for, relevant for es5 vs es6
     *            resolving
     * @return the resolved URL or the same as the input url if no translation
     *         was performed
     */
    public abstract String resolveResource(String url, WebBrowser browser);

    /**
     * Checks if the given URL has a themed version. If it does, returns the
     * untranslated URL for the themed resource.
     *
     * @param url
     *            the URL to lookup
     * @param browser
     *            the browser to use for lookup
     * @param theme
     *            the theme to check
     * @return an optional containing the untranslated (containing vaadin
     *         protocols) URL to the themed resource if such exists, an empty
     *         optional if the given resource has no themed version
     */
    public abstract Optional<String> getThemedUrl(String url,
            WebBrowser browser, AbstractTheme theme);

    /**
     * Gets the HTML import dependency cache that is used by this service.
     *
     * @return the HTML dependency cache
     */
    public DependencyTreeCache<String> getHtmlImportDependencyCache() {
        return htmlImportDependencyCache;
    }

    /**
     * Constructs {@link VaadinContext} for this service.
     *
     * This method will be called only once, upon first call to {@link #getContext()}.
     * @return Context. This may never be {@code null}.
     */
    protected abstract VaadinContext constructVaadinContext();

    /**
     * Returns {@link VaadinContext} for this service.
     * @return A non-null context instance.
     */
    public VaadinContext getContext() {
        if(vaadinContext == null) {
            vaadinContext = constructVaadinContext();
        }
        return vaadinContext;
    }
    /**
     *
     * Executes a {@code runnable} with a {@link VaadinService} available in the
     * {@link CurrentInstance} context.
     *
     * @param runnable
     *            command to execute
     */

    private void runWithServiceContext(Runnable runnable) {
        setCurrent(this);
        try {
            runnable.run();
        } finally {
            setCurrent(null);
        }
    }
}
