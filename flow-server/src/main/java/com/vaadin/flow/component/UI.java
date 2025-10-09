/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BaseJsonNode;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.internal.JavaScriptNavigationStateRenderer;
import com.vaadin.flow.component.internal.UIInternalUpdater;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree.ExecutionRegistration;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.LoadingIndicatorConfigurationMap;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.PollConfigurationMap;
import com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.EventUtil;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandlingCommand;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.shared.Registration;

/**
 * The topmost component in any component hierarchy. There is one UI for every
 * Vaadin instance in a browser window. A UI may either represent an entire
 * browser window (or tab) or some part of a html page where a Vaadin
 * application is embedded.
 * <p>
 * The UI is the server side entry point for various client side features that
 * are not represented as components added to a layout, e.g notifications, sub
 * windows, and executing javascript in the browser.
 * <p>
 * When a new UI instance is needed, typically because the user opens a URL in a
 * browser window which points to e.g. {@link VaadinServlet}, the UI mapped to
 * that servlet is opened. The selection is based on the <code>UI</code> init
 * parameter.
 * <p>
 * After a UI has been created by the application, it is initialized using
 * {@link #init(VaadinRequest)}.
 *
 * @see #init(VaadinRequest)
 *
 * @since 1.0
 */
@JsModule("@vaadin/common-frontend/ConnectionIndicator.js")
public class UI extends Component
        implements PollNotifier, HasComponents, RouterLayout {

    private static final String NULL_LISTENER = "Listener can not be 'null'";

    /**
     * The id of this UI, used to find the server side instance of the UI form
     * which a request originates. A negative value indicates that the UI id has
     * not yet been assigned by the Application.
     *
     * @see VaadinSession#getNextUIid()
     */
    private int uiId = -1;

    private boolean closing = false;

    private PushConfiguration pushConfiguration;

    private Locale locale = Locale.getDefault();

    private final UIInternals internals;

    private final Page page = new Page(this);

    /*
     * Despite section 6 of RFC 4122, this particular use of UUID *is* adequate
     * for security capabilities. Type 4 UUIDs contain 122 bits of random data,
     * and UUID.randomUUID() is defined to use a cryptographically secure random
     * generator.
     */
    private final String csrfToken = UUID.randomUUID().toString();

    /**
     * Creates a new empty UI.
     */
    public UI() {
        this(new UIInternalUpdater() {
        });
    }

    /**
     * Create a new empty UI with a custom {@link UIInternalUpdater}
     * implementation.
     *
     * @param internalsHandler
     *            an implementation of UIInternalsHandler.
     */
    protected UI(UIInternalUpdater internalsHandler) {
        super(null);
        internals = new UIInternals(this, internalsHandler);
        getNode().getFeature(ElementData.class).setTag("body");
        Component.setElement(this, Element.get(getNode()));
        pushConfiguration = new PushConfigurationImpl(this);
    }

    /**
     * Gets the VaadinSession to which this UI is attached.
     *
     * <p>
     * The method will return {@code null} if the UI is not currently attached
     * to a VaadinSession.
     *
     * <p>
     * Getting a null value is often a problem in constructors of regular
     * components and in the initializers of custom composite components. A
     * standard workaround is to use {@link VaadinSession#getCurrent()} to
     * retrieve the application instance that the current request relates to.
     * Another way is to move the problematic initialization to
     * {@link #onAttach(AttachEvent)}, as described in the documentation of the
     * method.
     *
     * @return the parent application of the component or <code>null</code>.
     * @see #onAttach(AttachEvent)
     */
    public VaadinSession getSession() {
        return internals.getSession();
    }

    /**
     * Gets the id of the UI, used to identify this UI within its application
     * when processing requests. The UI id should be present in every request to
     * the server that originates from this UI.
     * {@link VaadinService#findUI(VaadinRequest)} uses this id to find the
     * route to which the request belongs.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return this UI.
     *
     * @return the id of this UI
     */
    public int getUIId() {
        return uiId;
    }

    /**
     * Internal initialization method, should not be overridden. This method is
     * not declared as final because that would break compatibility with e.g.
     * CDI.
     * <p>
     * {@code appId} can be obtained by calling
     * {@code getService().getMainDivId(getSession(), getRequest())}.
     *
     * @param request
     *            the initialization request
     * @param uiId
     *            the id of the new ui
     * @param appId
     *            the application id
     *
     * @see #getUIId()
     */
    public void doInit(VaadinRequest request, int uiId, String appId) {
        if (this.uiId != -1) {
            String message = "This UI instance is already initialized (as UI id "
                    + this.uiId
                    + ") and can therefore not be initialized again (as UI id "
                    + uiId + "). ";

            if (getSession() != null
                    && !getSession().equals(VaadinSession.getCurrent())) {
                message += "Furthermore, it is already attached to another VaadinSession. ";
            }
            message += "Please make sure you are not accidentally reusing an old UI instance.";

            throw new IllegalStateException(message);
        }
        this.uiId = uiId;

        getInternals().setFullAppId(appId);

        if (this.isNavigationSupported()) {
            // Create flow reference for the client outlet element
            wrapperElement = new Element(getInternals().getContainerTag());

            // Connect server with client
            getElement().getStateProvider().appendVirtualChild(
                    getElement().getNode(), wrapperElement,
                    NodeProperties.INJECT_BY_ID, appId);

            getEventBus().addListener(BrowserLeaveNavigationEvent.class,
                    this::leaveNavigation);
            getEventBus().addListener(BrowserNavigateEvent.class,
                    this::browserNavigate);
            getEventBus().addListener(BrowserRefreshEvent.class,
                    this::browserRefresh);

        }

        // Add any dependencies from the UI class
        getInternals().addComponentDependencies(getClass());

        // Call the init overridden by the application developer
        init(request);
    }

    /**
     * Initializes this UI. This method is intended to be overridden by
     * subclasses to build the view if {@link Router} is not used. The method
     * can also be used to configure non-component functionality. Performing the
     * initialization in a constructor is not suggested as the state of the UI
     * is not properly set up when the constructor is invoked.
     * <p>
     * The provided {@link VaadinRequest} can be used to get information about
     * the request that caused this UI to be created.
     *
     * @param request
     *            the Vaadin request that caused this UI to be created
     */
    protected void init(VaadinRequest request) {
        // Does nothing by default
    }

    /**
     * Sets the thread local for the current UI. This method is used by the
     * framework to set the current application whenever a new request is
     * processed and it is cleared when the request has been processed.
     * <p>
     * The application developer can also use this method to define the current
     * UI outside the normal request handling, e.g. when initiating custom
     * background threads.
     * <p>
     * The UI is stored using a weak reference to avoid leaking memory in case
     * it is not explicitly cleared.
     *
     * @param ui
     *            the UI to register as the current UI
     *
     * @see #getCurrent()
     * @see ThreadLocal
     */
    public static void setCurrent(UI ui) {
        CurrentInstance.set(UI.class, ui);
    }

    /**
     * Gets the currently used UI. The current UI is automatically defined when
     * processing requests to the server. In other cases, (e.g. from background
     * threads), the current UI is not automatically defined.
     * <p>
     * The UI is stored using a weak reference to avoid leaking memory in case
     * it is not explicitly cleared.
     *
     * @return the current UI instance if available, otherwise <code>null</code>
     *
     * @see #setCurrent(UI)
     */
    public static UI getCurrent() {
        return CurrentInstance.get(UI.class);
    }

    /**
     * Marks this UI to be {@link #onDetach(DetachEvent) detached} from the
     * session at the end of the current request, or the next request if there
     * is no current request (if called from a background thread, for instance.)
     * <p>
     * The UI is detached after the response is sent, so in the current request
     * it can still update the client side normally. However, after the response
     * any new requests from the client side to this UI will cause an error, so
     * usually the client should be asked, for instance, to reload the page
     * (serving a fresh UI instance), to close the page, or to navigate
     * somewhere else.
     * <p>
     * Note that this method is strictly for users to explicitly signal the
     * framework that the UI should be detached. Overriding it is not a reliable
     * way to catch UIs that are to be detached. Instead,
     * {@code #onDetach(DetachEvent)} should be overridden.
     */
    public void close() {
        closing = true;

        // FIXME Send info to client

        PushConnection pushConnection = getInternals().getPushConnection();
        if (pushConnection != null) {
            // Push the Rpc to the client. The connection will be closed when
            // the UI is detached and cleaned up.

            // Can't use UI.push() directly since it checks for a valid session
            if (getSession() != null) {
                getSession().getService().runPendingAccessTasks(getSession());
            }
            pushConnection.push();
        }

    }

    /**
     * Returns whether this UI is marked as closed and is to be detached.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return this UI.
     *
     * @see #close()
     *
     * @return whether this UI is closing.
     */
    public boolean isClosing() {
        return closing;
    }

    /**
     * Called after the UI is added to the session. A UI instance is attached
     * exactly once, before its {@link #init(VaadinRequest) init} method is
     * called.
     *
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
    }

    /**
     * Called before the UI is removed from the session. A UI instance is
     * detached exactly once, either:
     * <ul>
     * <li>after it is explicitly {@link #close() closed}.
     * <li>when its session is closed or expires
     * <li>after three missed heartbeat requests
     * <li>when resynchronizing.
     * </ul>
     * <p>
     * Note that when a UI is detached, any changes made in the {@code detach}
     * methods of any children that would be communicated to the client are
     * silently ignored.
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
    }

    /**
     * Locks the session of this UI and runs the provided command right away.
     * <p>
     * It is generally recommended to use {@link #access(Command)} instead of
     * this method for accessing a session from a different thread as
     * {@link #access(Command)} can be used while holding the lock of another
     * session. To avoid causing deadlocks, this methods throws an exception if
     * it is detected than another session is also locked by the current thread.
     * <p>
     * This method behaves differently than {@link #access(Command)} in some
     * situations:
     * <ul>
     * <li>If the current thread is currently holding the lock of the session,
     * {@link #accessSynchronously(Command)} runs the task right away whereas
     * {@link #access(Command)} defers the task to a later point in time.</li>
     * <li>If some other thread is currently holding the lock for the session,
     * {@link #accessSynchronously(Command)} blocks while waiting for the lock
     * to be available whereas {@link #access(Command)} defers the task to a
     * later point in time.</li>
     * </ul>
     *
     *
     * @param command
     *            the command which accesses the UI
     * @throws UIDetachedException
     *             if the UI is not attached to a session (and locking can
     *             therefore not be done)
     * @throws IllegalStateException
     *             if the current thread holds the lock for another session
     *
     * @see #access(Command)
     * @see VaadinSession#accessSynchronously(Command)
     */
    public void accessSynchronously(Command command)
            throws UIDetachedException {
        // null detach handler -> throw UIDetachEvent
        accessSynchronously(command, null);
    }

    private static void handleAccessDetach(SerializableRunnable detachHandler) {
        if (detachHandler != null) {
            detachHandler.run();
        } else {
            throw new UIDetachedException();
        }
    }

    /*
     * Yes, we are mixing legacy Command with newer SerializableRunnable. This
     * is done for this internal method since it helps preserve old APIs as-is
     * while allowing new APIs to use newer conventions.
     */
    private void accessSynchronously(Command command,
            SerializableRunnable detachHandler) {

        Map<Class<?>, CurrentInstance> old = null;

        VaadinSession session = getSession();

        if (session == null) {
            handleAccessDetach(detachHandler);
            return;
        }

        VaadinService.verifyNoOtherSessionLocked(session);

        session.lock();
        try {
            if (getSession() == null) {
                // UI was detached after fetching the session but before we
                // acquired the lock.
                handleAccessDetach(detachHandler);
                return;
            }
            old = CurrentInstance.setCurrent(this);
            command.execute();
        } finally {
            session.unlock();
            if (old != null) {
                CurrentInstance.restoreInstances(old);
            }
        }

    }

    /**
     * Provides exclusive access to this UI from outside a request handling
     * thread.
     * <p>
     * The given command is executed while holding the session lock to ensure
     * exclusive access to this UI. If the session is not locked, the lock will
     * be acquired and the command is run right away. If the session is
     * currently locked, the command will be run before that lock is released.
     * <p>
     * RPC handlers for components inside this UI do not need to use this method
     * as the session is automatically locked by the framework during RPC
     * handling.
     * <p>
     * Please note that the command might be invoked on a different thread or
     * later on the current thread, which means that custom thread locals might
     * not have the expected values when the command is executed.
     * {@link UI#getCurrent()}, {@link VaadinSession#getCurrent()} and
     * {@link VaadinService#getCurrent()} are set according to this UI before
     * executing the command. Other standard CurrentInstance values such as
     * {@link VaadinService#getCurrentRequest()} and
     * {@link VaadinService#getCurrentResponse()} will not be defined.
     * <p>
     * The returned future can be used to check for task completion and to
     * cancel the task.
     *
     * @see #getCurrent()
     * @see #accessSynchronously(Command)
     * @see VaadinSession#access(Command)
     * @see VaadinSession#lock()
     *
     *
     * @param command
     *            the command which accesses the UI
     * @throws UIDetachedException
     *             if the UI is not attached to a session (and locking can
     *             therefore not be done)
     * @return a future that can be used to check for task completion and to
     *         cancel the task
     */
    public Future<Void> access(final Command command) {
        // null detach handler -> throw UIDetachEvent
        return access(command, null);
    }

    /*
     * Yes, we are mixing legacy Command with newer SerializableRunnable. This
     * is done for this internal method since it helps preserve old APIs as-is
     * while allowing new APIs to use newer conventions.
     */
    private Future<Void> access(Command command,
            SerializableRunnable detachHandler) {
        VaadinSession session = getSession();

        if (session == null) {
            handleAccessDetach(detachHandler);
            return null;
        }

        return session.access(new ErrorHandlingCommand() {
            @Override
            public void execute() {
                accessSynchronously(command, detachHandler);
            }

            @Override
            public void handleError(Exception exception) {
                try {
                    if (command instanceof ErrorHandlingCommand) {
                        ErrorHandlingCommand errorHandlingCommand = (ErrorHandlingCommand) command;
                        errorHandlingCommand.handleError(exception);
                    } else if (getSession() != null) {
                        final Map<Class<?>, CurrentInstance> map = CurrentInstance
                                .setCurrent(UI.this);
                        try {
                            getSession().getErrorHandler()
                                    .error(new ErrorEvent(exception));
                        } finally {
                            CurrentInstance.restoreInstances(map);
                        }
                    } else {
                        /*
                         * The session has expired after `ui.access` was called.
                         * It makes no sense to pollute the logs with a
                         * UIDetachedException at this point.
                         */
                        if (exception instanceof ExecutionException && exception
                                .getCause() instanceof UIDetachedException) {
                            getLogger().debug(exception.getMessage(),
                                    exception);
                        } else {
                            getLogger().error(exception.getMessage(),
                                    exception);
                        }
                    }
                } catch (Exception e) {
                    getLogger().error(e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Wraps the given access task as a runnable that runs the given task with
     * this UI locked. The wrapped task may be run synchronously or
     * asynchronously. If the UI is detached when the returned runnable is run,
     * the provided detach handler is run instead. If the provided detach
     * handler is <code>null</code>, the returned runnable may throw an
     * {@link UIDetachedException}.
     * <p>
     * This method can be used to create a callback that can be passed to an
     * external notifier that isn't aware of the synchronization needed to
     * update a UI instance.
     *
     * @param accessTask
     *            the task that updates this UI, not <code>null</code>
     * @param detachHandler
     *            the callback that will be invoked if the UI is detached, or
     *            <code>null</code> as described above
     * @return a runnable that will run either the access task or the detach
     *         handler, possibly asynchronously
     */
    public SerializableRunnable accessLater(SerializableRunnable accessTask,
            SerializableRunnable detachHandler) {
        Objects.requireNonNull(accessTask, "Access task cannot be null");

        return () -> access(accessTask::run, detachHandler);
    }

    /**
     * Wraps the given access task as a consumer that passes a value to the
     * given task with this UI locked. The wrapped task may be run synchronously
     * or asynchronously. If the UI is detached when the returned consumer is
     * run, the provided detach handler is run instead. If the provided detach
     * handler is <code>null</code>, the returned runnable may throw an
     * {@link UIDetachedException}.
     * <p>
     * This method can be used to create a callback that can be passed to an
     * external notifier that isn't aware of the synchronization needed to
     * update a UI instance.
     *
     * @param accessTask
     *            the task that updates this UI, not <code>null</code>
     * @param detachHandler
     *            the callback that will be invoked if the UI is detached, or
     *            <code>null</code> as described above
     * @return a consumer that will run either the access task or the detach
     *         handler, possibly asynchronously
     */
    public <T> SerializableConsumer<T> accessLater(
            SerializableConsumer<T> accessTask,
            SerializableRunnable detachHandler) {
        Objects.requireNonNull(accessTask, "Access task cannot be null");

        return value -> access(() -> accessTask.accept(value), detachHandler);
    }

    /**
     * Sets the interval with which the UI should poll the server to see if
     * there are any changes. Polling is disabled by default.
     * <p>
     * Note that it is possible to enable push and polling at the same time but
     * it should not be done to avoid excessive server traffic.
     * <p>
     * Add-on developers should note that this method is only meant for the
     * application developer. An add-on should not set the poll interval
     * directly, rather instruct the user to set it.
     *
     * @param intervalInMillis
     *            The interval (in ms) with which the UI should poll the server
     *            or -1 to disable polling
     */
    public void setPollInterval(int intervalInMillis) {
        getNode().getFeature(PollConfigurationMap.class)
                .setPollInterval(intervalInMillis);
    }

    /**
     * Returns the interval with which the UI polls the server.
     *
     * @return The interval (in ms) with which the UI polls the server or -1 if
     *         polling is disabled
     */
    public int getPollInterval() {
        return getNode().getFeature(PollConfigurationMap.class)
                .getPollInterval();
    }

    /**
     * Retrieves the object used for configuring the loading indicator.
     *
     * @return The instance used for configuring the loading indicator
     */
    public LoadingIndicatorConfiguration getLoadingIndicatorConfiguration() {
        return getNode().getFeature(LoadingIndicatorConfigurationMap.class);
    }

    /**
     * Pushes the pending changes and client RPC invocations of this UI to the
     * client-side.
     * <p>
     * If push is enabled, but the push connection is not currently open, the
     * push will be done when the connection is established.
     * <p>
     * As with all UI methods, the session must be locked when calling this
     * method. It is also recommended that {@link UI#getCurrent()} is set up to
     * return this UI since writing the response may invoke logic in any
     * attached component or extension. The recommended way of fulfilling these
     * conditions is to use {@link #access(Command)}.
     *
     * @throws IllegalStateException
     *             if push is disabled.
     * @throws UIDetachedException
     *             if this UI is not attached to a session.
     *
     * @see #getPushConfiguration()
     *
     */
    public void push() {
        VaadinSession session = getSession();

        if (session == null) {
            throw new UIDetachedException("Cannot push a detached UI");
        }
        session.checkHasLock();

        if (!getPushConfiguration().getPushMode().isEnabled()) {
            throw new IllegalStateException("Push not enabled");
        }

        PushConnection pushConnection = getInternals().getPushConnection();
        assert pushConnection != null;

        /*
         * Purge the pending access queue as it might mark a connector as dirty
         * when the push would otherwise be ignored because there are no changes
         * to push.
         */
        session.getService().runPendingAccessTasks(session);

        if (!getInternals().isDirty()
                || getInternals().getStateTree().isPreparingForResync()) {
            // Do not push: there is nothing to push, or UI is preparing for
            // resync and should not asynchronously dispatch messages to the
            // client until the process is completed
            return;
        }

        pushConnection.push();
    }

    /**
     * Retrieves the object used for configuring the push channel.
     * <p>
     * Note that you cannot change push parameters on the fly, you need to
     * configure the push channel at the same time (in the same request) it is
     * enabled.
     *
     * @return The instance used for push configuration
     */
    public PushConfiguration getPushConfiguration() {
        return pushConfiguration;
    }

    /**
     * Retrieves the object used for configuring the reconnect dialog.
     *
     * @return The instance used for reconnect dialog configuration
     */
    public ReconnectDialogConfiguration getReconnectDialogConfiguration() {
        return getNode().getFeature(ReconnectDialogConfigurationMap.class);
    }

    Logger getLogger() {
        return LoggerFactory.getLogger(UI.class.getName());
    }

    /**
     * Gets the locale for this UI. The default locale is based on the session's
     * locale, which is in turn determined in different ways depending on
     * whether a {@link I18NProvider} is available.
     * <p>
     * If an i18n provider is available, the locale is determined by selecting
     * the locale from {@link I18NProvider#getProvidedLocales()} that best
     * matches the user agent preferences (i.e. the <code>Accept-Language</code>
     * header). If an exact match is found, then that locale is used. Otherwise,
     * the matching logic looks for the first provided locale that uses the same
     * language regardless of the country. If no other match is found, then
     * {@link I18NProvider#getDefaultLocale()} is used.
     * <p>
     * If no i18n provider is available, then the {@link Locale#getDefault()
     * default JVM locale} is used as the default locale.
     *
     * @return the locale in use, not <code>null</code>
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale for this UI.
     * <p>
     * Note that {@link VaadinSession#setLocale(Locale)} will set the locale for
     * all UI instances in that session, and might thus override any custom
     * locale previous set for a specific UI.
     *
     * @param locale
     *            the locale to use, not null
     */
    public void setLocale(Locale locale) {
        assert locale != null : "Null locale is not supported!";
        if (!this.locale.equals(locale)) {
            this.locale = locale;
            EventUtil.informLocaleChangeObservers(this);
        }
    }

    /**
     * Sets the direction for the UI.
     * <p>
     * If you need the direction to update automatically upon {@link Locale}
     * change, make the main layout implement
     * {@link com.vaadin.flow.i18n.LocaleChangeObserver} and call this method
     * from the
     * {@link com.vaadin.flow.i18n.LocaleChangeObserver#localeChange(LocaleChangeEvent)}
     * implementation.
     *
     * @param direction
     *            the direction to use, not {@code null}
     */
    public void setDirection(Direction direction) {
        Objects.requireNonNull(direction, "Direction cannot be null");
        getPage().executeJs("document.dir = $0", direction.getClientName());
    }

    /**
     * Gets the element for this UI.
     * <p>
     * The UI element corresponds to the {@code <body>} tag on the page
     *
     * @return the element for this UI
     */
    @Override
    public Element getElement() {
        return Element.get(getNode());
    }

    /**
     * Gets the state node for this UI.
     *
     * @return the state node for the UI, in practice the state tree root node
     */
    private StateNode getNode() {
        return getInternals().getStateTree().getRootNode();
    }

    /**
     * Gets the framework data object for this UI.
     *
     * This method is for internal use only.
     *
     * @return the framework data object
     */
    public UIInternals getInternals() {
        return internals;
    }

    /**
     * Gets the object representing the page on which this UI exists.
     *
     * @return an object representing the page on which this UI exists
     */
    public Page getPage() {
        return page;
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * <p>
     * If the view change actually happens (e.g. the view itself doesn't cancel
     * the navigation), all navigation listeners are notified and a reference of
     * the new view is returned for additional configuration.
     *
     *
     * @param navigationTarget
     *            navigation target to navigate to
     * @throws IllegalArgumentException
     *             if navigationTarget is a {@link HasUrlParameter} with a
     *             mandatory parameter.
     * @throws NotFoundException
     *             in case there is no route defined for the given
     *             navigationTarget.
     * @return the view instance, if navigation actually happened
     * @see #navigate(Class, Object)
     * @see #navigate(Class, RouteParameters)
     */
    public <T extends Component> Optional<T> navigate(
            Class<T> navigationTarget) {
        return navigate(navigationTarget, RouteParameters.empty());
    }

    private <T extends Component> Optional<T> findCurrentNavigationTarget(
            Class<T> navigationTarget) {
        List<HasElement> activeRouterTargetsChain = getInternals()
                .getActiveRouterTargetsChain();
        for (HasElement element : activeRouterTargetsChain) {
            if (navigationTarget.isAssignableFrom(element.getClass())) {
                return Optional.of((T) element);
            }
        }
        return Optional.empty();
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target with the specified parameter. The parameter needs to be the same
     * as defined in the route target HasUrlParameter.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * <p>
     * Note! A {@code null} parameter will be handled the same as
     * navigate(navigationTarget) and will throw an exception if HasUrlParameter
     * is not @OptionalParameter or @WildcardParameter.
     * <p>
     * If the view change actually happens (e.g. the view itself doesn't cancel
     * the navigation), all navigation listeners are notified and a reference of
     * the new view is returned for additional configuration.
     *
     * @param navigationTarget
     *            navigation target to navigate to
     * @param parameter
     *            parameter to pass to view
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     * @return the view instance, if navigation actually happened
     * @throws IllegalArgumentException
     *             if a {@code null} parameter is given while navigationTarget's
     *             parameter is not annotated with @OptionalParameter
     *             or @WildcardParameter.
     * @throws NotFoundException
     *             in case there is no route defined for the given
     *             navigationTarget matching the parameters.
     */
    @SuppressWarnings("unchecked")
    public <T, C extends Component & HasUrlParameter<T>> Optional<C> navigate(
            Class<? extends C> navigationTarget, T parameter) {
        navigate(navigationTarget,
                HasUrlParameterFormat.getParameters(parameter));
        return (Optional<C>) findCurrentNavigationTarget(navigationTarget);
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target with the specified parameters. The parameters needs to comply with
     * the ones defined in one of the {@link com.vaadin.flow.router.Route} or
     * {@link com.vaadin.flow.router.RouteAlias} annotating the navigationTarget
     * and with any {@link com.vaadin.flow.router.RoutePrefix} annotating the
     * parent layouts of the navigationTarget.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * <p>
     * If the view change actually happens (e.g. the view itself doesn't cancel
     * the navigation), all navigation listeners are notified and a reference of
     * the new view is returned for additional configuration.
     *
     * @param navigationTarget
     *            navigation target to navigate to.
     * @param parameters
     *            parameters to pass to view.
     * @return the view instance, if navigation actually happened
     * @throws IllegalArgumentException
     *             if navigationTarget is a {@link HasUrlParameter} with a
     *             mandatory parameter, but parameters argument doesn't provide
     *             {@link HasUrlParameterFormat#PARAMETER_NAME} parameter.
     * @throws NotFoundException
     *             in case there is no route defined for the given
     *             navigationTarget matching the parameters.
     */
    public <T extends Component> Optional<T> navigate(Class<T> navigationTarget,
            RouteParameters parameters) {
        RouteConfiguration configuration = RouteConfiguration
                .forRegistry(getInternals().getRouter().getRegistry());
        navigate(configuration.getUrl(navigationTarget, parameters));
        return findCurrentNavigationTarget(navigationTarget);
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target with the specified parameters. The parameters needs to comply with
     * the ones defined in one of the {@link com.vaadin.flow.router.Route} or
     * {@link com.vaadin.flow.router.RouteAlias} annotating the navigationTarget
     * and with any {@link com.vaadin.flow.router.RoutePrefix} annotating the
     * parent layouts of the navigationTarget.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * <p>
     * If the view change actually happens (e.g. the view itself doesn't cancel
     * the navigation), all navigation listeners are notified and a reference of
     * the new view is returned for additional configuration.
     *
     * @param navigationTarget
     *            navigation target to navigate to.
     * @param parameters
     *            parameters to pass to view.
     * @return the view instance, if navigation actually happened
     * @throws IllegalArgumentException
     *             if navigationTarget is a {@link HasUrlParameter} with a
     *             mandatory parameter, but parameters argument doesn't provide
     *             {@link HasUrlParameterFormat#PARAMETER_NAME} parameter.
     * @throws NotFoundException
     *             in case there is no route defined for the given
     *             navigationTarget matching the parameters.
     */
    public <T extends Component> Optional<T> navigate(Class<T> navigationTarget,
            RouteParam... parameters) {
        return navigate(navigationTarget, new RouteParameters(parameters));
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target with the specified parameter. The parameter needs to be the same
     * as defined in the route target HasUrlParameter.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * <p>
     * Note! A {@code null} parameter will be handled the same as
     * navigate(navigationTarget) and will throw an exception if HasUrlParameter
     * is not @OptionalParameter or @WildcardParameter.
     * <p>
     * If the view change actually happens (e.g. the view itself doesn't cancel
     * the navigation), all navigation listeners are notified and a reference of
     * the new view is returned for additional configuration.
     *
     * @param navigationTarget
     *            navigation target to navigate to
     * @param parameter
     *            route parameter to pass to view
     * @param queryParameters
     *            additional query parameters to pass to view
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     * @return the view instance, if navigation actually happened
     * @throws IllegalArgumentException
     *             if a {@code null} parameter is given while navigationTarget's
     *             parameter is not annotated with @OptionalParameter
     *             or @WildcardParameter.
     * @throws NotFoundException
     *             in case there is no route defined for the given
     *             navigationTarget matching the parameters.
     */
    @SuppressWarnings("unchecked")
    public <T, C extends Component & HasUrlParameter<T>> Optional<C> navigate(
            Class<? extends C> navigationTarget, T parameter,
            QueryParameters queryParameters) {

        RouteConfiguration configuration = RouteConfiguration
                .forRegistry(getInternals().getRouter().getRegistry());
        RouteParameters parameters = HasUrlParameterFormat
                .getParameters(parameter);
        String url = configuration.getUrl(navigationTarget, parameters);
        getInternals().getRouter().navigate(this,
                new Location(url, queryParameters),
                NavigationTrigger.UI_NAVIGATE);
        return (Optional<C>) findCurrentNavigationTarget(navigationTarget);
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target with the specified parameters. The route parameters needs to
     * comply with the ones defined in one of the
     * {@link com.vaadin.flow.router.Route} or
     * {@link com.vaadin.flow.router.RouteAlias} annotating the navigationTarget
     * and with any {@link com.vaadin.flow.router.RoutePrefix} annotating the
     * parent layouts of the navigationTarget.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * <p>
     * If the view change actually happens (e.g. the view itself doesn't cancel
     * the navigation), all navigation listeners are notified and a reference of
     * the new view is returned for additional configuration.
     *
     * @param navigationTarget
     *            navigation target to navigate to
     * @param routeParameter
     *            route parameters to pass to view
     * @param queryParameters
     *            additional query parameters to pass to view
     * @param <C>
     *            navigation target type
     * @return the view instance, if navigation actually happened
     * @throws IllegalArgumentException
     *             if a {@code null} parameter is given while navigationTarget's
     *             parameter is not annotated with @OptionalParameter
     *             or @WildcardParameter.
     * @throws NotFoundException
     *             in case there is no route defined for the given
     *             navigationTarget matching the parameters.
     */
    @SuppressWarnings("unchecked")
    public <C extends Component> Optional<C> navigate(
            Class<? extends C> navigationTarget, RouteParameters routeParameter,
            QueryParameters queryParameters) {
        RouteConfiguration configuration = RouteConfiguration
                .forRegistry(getInternals().getRouter().getRegistry());
        String url = configuration.getUrl(navigationTarget, routeParameter);
        getInternals().getRouter().navigate(this,
                new Location(url, queryParameters),
                NavigationTrigger.UI_NAVIGATE);
        return (Optional<C>) findCurrentNavigationTarget(navigationTarget);
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target and query parameters.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * <p>
     * If the view change actually happens (e.g. the view itself doesn't cancel
     * the navigation), all navigation listeners are notified and a reference of
     * the new view is returned for additional configuration.
     *
     * @param navigationTarget
     *            navigation target to navigate to
     * @param queryParameters
     *            additional query parameters to pass to view
     * @param <T>
     *            navigation target type
     * @return the view instance, if navigation actually happened
     * @throws NotFoundException
     *             in case there is no route defined for the given
     *             navigationTarget matching the parameters.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> navigate(
            Class<? extends T> navigationTarget,
            QueryParameters queryParameters) {

        RouteConfiguration configuration = RouteConfiguration
                .forRegistry(getInternals().getRouter().getRegistry());
        String url = configuration.getUrl(navigationTarget,
                RouteParameters.empty());
        getInternals().getRouter().navigate(this,
                new Location(url, queryParameters),
                NavigationTrigger.UI_NAVIGATE);
        return (Optional<T>) findCurrentNavigationTarget(navigationTarget);
    }

    /**
     * Updates this UI to show the view corresponding to the given location. The
     * location must be a relative path without any ".." segments.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     *
     * @see #navigate(String, QueryParameters)
     * @see Router#navigate(UI, Location, NavigationTrigger)
     *
     * @param location
     *            the location to navigate to, not {@code null}
     * @throws NullPointerException
     *             if the location is null.
     */
    public void navigate(String location) {
        navigate(location, QueryParameters.empty());
    }

    /**
     * Updates this UI to show the view corresponding to the given location and
     * query parameters. The location must be a relative path without any ".."
     * segments.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     *
     * @see #navigate(String)
     * @see Router#navigate(UI, Location, NavigationTrigger)
     *
     * @param locationString
     *            the location to navigate to, not {@code null}
     * @param queryParameters
     *            query parameters that are used for navigation, not
     *            {@code null}
     * @throws NullPointerException
     *             if the location or queryParameters are null.
     */
    public void navigate(String locationString,
            QueryParameters queryParameters) {
        Objects.requireNonNull(locationString, "Location must not be null");
        Objects.requireNonNull(queryParameters,
                "Query parameters must not be null");
        Location location = new Location(locationString, queryParameters);

        // There is an in-progress navigation or there are no changes,
        // prevent looping
        if (navigationInProgress
                || (getInternals().hasLastHandledLocation() && sameLocation(
                        getInternals().getLastHandledLocation(), location))) {
            return;
        }

        navigationInProgress = true;
        try {
            Optional<NavigationState> navigationState = getInternals()
                    .getRouter().resolveNavigationTarget(location);

            if (navigationState.isPresent()) {
                // Navigation can be done in server side without extra
                // round-trip
                handleNavigation(location, navigationState.get(),
                        NavigationTrigger.UI_NAVIGATE);
                if (getForwardToClientUrl() != null) {
                    // Server is forwarding to a client route from a
                    // BeforeEnter.
                    navigateToClient(getForwardToClientUrl());
                }
            } else {
                // Server cannot resolve navigation, let client-side to
                // handle it.
                navigateToClient(location.getPathWithQueryParameters());
            }
        } finally {
            navigationInProgress = false;
        }
    }

    /**
     * Re-navigates to the current route. Also re-instantiates the route target
     * component, and optionally all layouts in the route chain.
     *
     * @param refreshRouteChain
     *            {@code true} to refresh all layouts in the route chain,
     *            {@code false} to only refresh the route instance
     */
    public void refreshCurrentRoute(boolean refreshRouteChain) {
        getInternals().refreshCurrentRoute(refreshRouteChain);
    }

    private void browserRefresh(BrowserRefreshEvent event) {
        refreshCurrentRoute(event.refreshRouteChain);
    }

    /**
     * Returns true if this UI instance supports navigation.
     *
     * @return true if this UI instance supports navigation, otherwise false.
     */
    public boolean isNavigationSupported() {
        // By default any UI supports navigation. Override this to return false
        // if navigation is not supported.
        return true;
    }

    /**
     * Returns the currently active route aka navigation target shown in this
     * UI.
     * <p>
     * Note, that certain UIs, like embedded apps, don't support routing and for
     * those an exception will be thrown.
     * <p>
     * Also, the current route might not be initialized if this method is called
     * while still building the view chain, for example in the constructor of
     * layouts. Thus, consider postponing the usage of this method to for
     * example AfterNavigationEvent.
     *
     * @return the currently active route instance if available
     * @throws IllegalStateException
     *             if current view is not yet available
     */
    public Component getCurrentView() {
        if (getInternals().getActiveRouterTargetsChain().isEmpty()) {
            throw new IllegalStateException(
                    "Routing is not in use or not yet initialized. If you are not using embedded UI, try postponing the call to an onAttach method or to an AfterNavigationEvent listener.");
        }
        return (Component) getInternals().getActiveRouterTargetsChain().get(0);
    }

    /**
     * Registers a task to be executed before the response is sent to the
     * client. The tasks are executed in order of registration. If tasks
     * register more tasks, they are executed after all already registered tasks
     * for the moment.
     * <p>
     * Example: three tasks are submitted, {@code A}, {@code B} and {@code C},
     * where {@code B} produces two more tasks during execution, {@code D} and
     * {@code E}. The resulting execution would be {@code ABCDE}.
     * <p>
     * If the {@link Component} related to the task is not attached to the
     * document by the time the task is evaluated, the execution is postponed to
     * before the next response.
     * <p>
     * The task receives a {@link ExecutionContext} as parameter, which contains
     * information about the component state before the response.
     *
     * @param component
     *            the Component relevant for the execution. Can not be
     *            <code>null</code>
     *
     * @param execution
     *            the task to be executed. Can not be <code>null</code>
     *
     * @return a registration that can be used to cancel the execution of the
     *         task
     * @throws IllegalArgumentException
     *             if the given component doesn't belong to this UI
     */
    public ExecutionRegistration beforeClientResponse(Component component,
            SerializableConsumer<ExecutionContext> execution)
            throws IllegalArgumentException {

        if (component == null) {
            throw new IllegalArgumentException(
                    "The 'component' parameter may not be null");
        }
        if (execution == null) {
            throw new IllegalArgumentException(
                    "The 'execution' parameter may not be null");
        }

        Optional<UI> componentUi = component.getUI();
        if (componentUi.isPresent() && componentUi.get() != this) {
            throw new IllegalArgumentException(
                    "The given component doesn't belong to the UI the task to be executed on");
        }

        return internals.getStateTree().beforeClientResponse(
                component.getElement().getNode(), execution);
    }

    /**
     * Adds the given components to the UI.
     * <p>
     * The components' elements are attached to the UI element (the body tag).
     *
     * @param components
     *            the components to add
     */
    // Overridden just to mention UI and <body> in the javadocs
    @Override
    public void add(Component... components) {
        HasComponents.super.add(components);
    }

    @Override
    public Optional<UI> getUI() {
        return Optional.of(this);
    }

    /**
     * Add a listener that will be informed when a new set of components are
     * going to be attached.
     * <p>
     * By the time the components are going to be attached, their state is
     * already calculated, consider this when using the listener.
     * <p>
     * Listeners will be executed before any found observers.
     *
     * @param listener
     *            the before enter listener
     * @return handler to remove the event listener
     */
    public Registration addBeforeEnterListener(BeforeEnterListener listener) {
        Objects.requireNonNull(listener, NULL_LISTENER);
        return internals.addBeforeEnterListener(listener);
    }

    /**
     * Add a listener that will be informed when old components are detached.
     * <p>
     * Listeners will be executed before any found observers.
     * <p>
     * If a route target is left for reasons not under the control of the
     * navigator (for instance using
     * {@link com.vaadin.flow.component.page.Page#setLocation(URI)}, typing a
     * URL into the address bar, or closing the browser), listeners are not
     * called.
     *
     * @param listener
     *            the before leave listener
     * @return handler to remove the event listener
     */
    public Registration addBeforeLeaveListener(BeforeLeaveListener listener) {
        Objects.requireNonNull(listener, NULL_LISTENER);
        return internals.addBeforeLeaveListener(listener);
    }

    /**
     * Add a listener that will be informed when new components have been
     * attached and all navigation tasks have resolved.
     * <p>
     * Listeners will be executed before any found observers.
     *
     * @param listener
     *            the after navigation listener
     * @return handler to remove the event listener
     */
    public Registration addAfterNavigationListener(
            AfterNavigationListener listener) {
        Objects.requireNonNull(listener, NULL_LISTENER);
        return internals.addAfterNavigationListener(listener);
    }

    /**
     * Get all the registered listeners of the given navigation handler type.
     *
     * @param navigationHandler
     *            navigation handler type to get listeners for
     * @param <E>
     *            the handler type
     * @return unmodifiable list of registered listeners for navigation handler
     */
    public <E> List<E> getNavigationListeners(Class<E> navigationHandler) {
        return internals.getListeners(navigationHandler);
    }

    /**
     * Registers a global shortcut tied to the {@code UI} which executes the
     * given {@link Command} when invoked.
     * <p>
     * Returns {@link ShortcutRegistration} which can be used to fluently
     * configure the shortcut. The shortcut will be present until
     * {@link ShortcutRegistration#remove()} is called.
     *
     * @param command
     *            code to execute when the shortcut is invoked. Cannot be null
     * @param key
     *            primary {@link Key} used to trigger the shortcut. Cannot be
     *            null
     * @param keyModifiers
     *            {@link KeyModifier KeyModifiers} which also need to be pressed
     *            for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring the shortcut and
     *         removing
     * @see #addShortcutListener(ShortcutEventListener, Key, KeyModifier...) for
     *      registering a listener which receives a {@link ShortcutEvent}
     * @see Shortcuts for a more generic way to add a shortcut
     */
    public ShortcutRegistration addShortcutListener(Command command, Key key,
            KeyModifier... keyModifiers) {
        if (command == null) {
            throw new IllegalArgumentException(
                    String.format(Shortcuts.NULL, "command"));
        }
        if (key == null) {
            throw new IllegalArgumentException(
                    String.format(Shortcuts.NULL, "key"));
        }
        return new ShortcutRegistration(this, () -> new Component[] { this },
                event -> command.execute(), key).withModifiers(keyModifiers);
    }

    /**
     * Registers a global shortcut tied to the {@code UI} which executes the
     * given {@link ComponentEventListener} when invoked.
     * <p>
     * Returns {@link ShortcutRegistration} which can be used to fluently
     * configure the shortcut. The shortcut will be present until
     * {@link ShortcutRegistration#remove()} is called.
     *
     * @param listener
     *            listener to execute when the shortcut is invoked. Receives a
     *            {@link ShortcutEvent}. Cannot be null
     * @param key
     *            primary {@link Key} used to trigger the shortcut
     * @param keyModifiers
     *            {@link KeyModifier KeyModifiers} which also need to be pressed
     *            for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring the shortcut and
     *         removing
     * @see Shortcuts for a more generic way to add a shortcut
     */
    public ShortcutRegistration addShortcutListener(
            ShortcutEventListener listener, Key key,
            KeyModifier... keyModifiers) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    String.format(Shortcuts.NULL, "listener"));
        }
        if (key == null) {
            throw new IllegalArgumentException(
                    String.format(Shortcuts.NULL, "key"));
        }
        return new ShortcutRegistration(this, () -> new Component[] { this },
                listener, key).withModifiers(keyModifiers);
    }

    /**
     * Add a listener that will be informed when this UI received a heartbeat
     * from the client-side.
     * <p>
     * Heartbeat requests are periodically sent by the client-side to inform the
     * server that the UI sending the heartbeat is still alive (the browser
     * window is open, the connection is up) even when there are no UIDL
     * requests for a prolonged period of time. UIs that do not receive either
     * heartbeat or UIDL requests are eventually removed from the session and
     * garbage collected.
     *
     * @param listener
     *            the heartbeat listener
     * @return handler to remove the heartbeat listener
     */
    public Registration addHeartbeatListener(HeartbeatListener listener) {
        Objects.requireNonNull(listener, NULL_LISTENER);
        return internals.addHeartbeatListener(listener);
    }

    /**
     * Gets the drag source of an active HTML5 drag event.
     * <p>
     * <em>NOTE: the generic drag and drop feature for Flow is available in
     * another artifact, {@code flow-dnd} for now.</em>
     *
     * @return Extension of the drag source component if the drag event is
     *         active and originated from this UI, {@literal null} otherwise.
     * @since 2.0
     */
    public Component getActiveDragSourceComponent() {
        return getInternals().getActiveDragSourceComponent();
    }

    /**
     * Gets the CSRF token (synchronizer token pattern) that is used to protect
     * against Cross Site Request Forgery attacks.
     *
     * @return the csrf token string
     * @since 2.0
     */
    public String getCsrfToken() {
        return csrfToken;
    }

    /**
     * Adds the given component as a modal child to the UI, making the UI and
     * all other (existing) components added to the UI impossible for the user
     * to interact with. Using {@link ModalityMode#STRICT} mode. This is useful
     * for modal dialogs which should make the UI in the background inert. Note
     * that this only prevents user interaction, but doesn't show a modality
     * curtain or change the visible state of the components in the UI - that
     * should be handled by the component separately. Thus, this is purely a
     * server side feature.
     * <p>
     * When the modal component is removed the UI and its other children are no
     * longer inert, unless there was another component added as modal before.
     *
     * @param component
     *            the modal component to add
     * @see #setChildComponentModal(Component, boolean)
     * @see #setChildComponentModal(Component, ModalityMode)
     */
    public void addModal(Component component) {
        add(component);
        setChildComponentModal(component, true);
    }

    /**
     * Makes the child component modal or modeless (i.e.
     * {@link ModalityMode#STRICT} or {@link ModalityMode#MODELESS}). The
     * component needs to be a child of this UI. By default, all child
     * components are modeless. Note that calling this doesn't show a modality
     * curtain or change the visible state of the components in the UI - that
     * should be handled by the component separately. Thus, this is purely a
     * server side feature.
     *
     * @param childComponent
     *            the child component to change state for
     * @param modal
     *            {@code true} for modal, {@code false} for modeless
     * @see #setChildComponentModal(Component, ModalityMode)
     */
    /*
     * TODO decide and document whether resize listener still works for UI even
     * when it is inert.
     */
    public void setChildComponentModal(Component childComponent,
            boolean modal) {
        setChildComponentModal(childComponent,
                modal ? ModalityMode.STRICT : ModalityMode.MODELESS);
    }

    /**
     * Change the child component server side modality by modality mode:
     * {@link ModalityMode#STRICT}, {@link ModalityMode#VISUAL}, or
     * {@link ModalityMode#MODELESS}. The component needs to be a child of this
     * UI. By default, all child components are modeless. Note that calling this
     * doesn't show a modality curtain or change the visible state of the
     * components in the UI - that should be handled by the component
     * separately. Thus, this is purely a server side feature.
     *
     * @param childComponent
     *            the child component to change state for
     * @param mode
     *            the modality mode, not null
     */
    public void setChildComponentModal(Component childComponent,
            ModalityMode mode) {
        Objects.requireNonNull(childComponent,
                "Given child component may not be null");
        Objects.requireNonNull(mode, "Given modality mode may not be null");
        final Optional<UI> ui = childComponent.getUI();
        if (ui.isPresent() && !ui.get().equals(this)) {
            throw new IllegalStateException(
                    "Given component is not a child in this UI. "
                            + "Add it first as a child of the UI so it is "
                            + "attached or just use addModal(component) when using ModalityMode.STRICT.");
        }
        if (mode == ModalityMode.STRICT) {
            getInternals().setChildModal(childComponent);
        } else {
            getInternals().setChildModeless(childComponent);
        }
    }

    /**
     * Check if UI has a defined modal component.
     *
     * @return {@code true} if a modal component has been set
     */
    public boolean hasModalComponent() {
        return getInternals().hasModalComponent();
    }

    /**
     * Add component as child to modal component if one is active. Else it will
     * be added to the UI normally.
     * <p>
     * This is meant to be used with components that are not added as part of a
     * layout, like dialog, so that they are interactive when a modal component
     * opens up an overlay component.
     *
     * @param component
     *            component to add to modal component
     */
    public void addToModalComponent(Component component) {
        if (hasModalComponent()) {
            final Component activeModalComponent = getInternals()
                    .getActiveModalComponent();
            if (activeModalComponent instanceof HasComponents) {
                ((HasComponents) activeModalComponent).add(component);
            } else {
                activeModalComponent.getElement()
                        .appendChild(component.getElement());
            }
        } else {
            add(component);
        }
    }

    @Override
    public Stream<Component> getChildren() {
        // server-side routing
        if (wrapperElement == null) {
            return super.getChildren();
        }

        // #9069 with client-side routing, since routing component is a virtual
        // child, its children need to be included separately (there should only
        // be one)
        Stream.Builder<Component> childComponents = Stream.builder();
        wrapperElement.getChildren().forEach(childElement -> ComponentUtil
                .findComponents(childElement, childComponents::add));
        super.getChildren().forEach(childComponents::add);
        return childComponents.build();
    }

    /**
     * Gets the location of the currently shown view. The location is relative
     * the servlet mapping used for serving the related UI.
     *
     * @return the view location, not <code>null</code>
     */
    public Location getActiveViewLocation() {
        return getInternals().getActiveViewLocation();
    }

    /**
     * Gets the currently active router target and parent layouts.
     *
     * @return a list of active router target and parent layout instances,
     *         starting from the innermost part
     */
    public List<HasElement> getActiveRouterTargetsChain() {
        return getInternals().getActiveRouterTargetsChain();
    }

    static final String SERVER_CONNECTED = "this.serverConnected($0)";
    public static final String CLIENT_NAVIGATE_TO = """
            const url = new URL($0, document.baseURI);
            url["clientNavigation"] = true;
            window.dispatchEvent(new CustomEvent('vaadin-router-go', { detail: url}));
            """;

    public Element wrapperElement;
    private NavigationState clientViewNavigationState;
    private boolean navigationInProgress = false;

    private String forwardToClientUrl = null;

    private boolean firstNavigation = true;

    /**
     * Gets the new forward url.
     *
     * @return the new forward url
     */
    public String getForwardToClientUrl() {
        return forwardToClientUrl;
    }

    @DomEvent(BrowserLeaveNavigationEvent.EVENT_NAME)
    public static class BrowserLeaveNavigationEvent extends ComponentEvent<UI> {
        public static final String EVENT_NAME = "ui-leave-navigation";
        private final String route;
        private final String query;

        /**
         * Creates a new event instance.
         *
         * @param route
         *            the route the user is navigating to.
         * @param query
         *            the query string the user is navigating to.
         */
        public BrowserLeaveNavigationEvent(UI source, boolean fromClient,
                @EventData("route") String route,
                @EventData("query") String query) {
            super(source, true);
            this.route = route;
            this.query = query;
        }
    }

    @DomEvent(BrowserNavigateEvent.EVENT_NAME)
    public static class BrowserNavigateEvent extends ComponentEvent<UI> {
        public static final String EVENT_NAME = "ui-navigate";

        private final String route;
        private final String query;
        private final String appShellTitle;
        private final JsonNode historyState;
        private final String trigger;

        /**
         * Creates a new event instance.
         *
         * @param route
         *            flow route path that should be attached to the client
         *            element
         * @param query
         *            flow route query string
         * @param appShellTitle
         *            client side title of the application shell
         * @param historyState
         *            client side history state value
         * @param trigger
         *            navigation trigger
         *
         */
        public BrowserNavigateEvent(UI source, boolean fromClient,
                @EventData("route") String route,
                @EventData("query") String query,
                @EventData("appShellTitle") String appShellTitle,
                @EventData("historyState") JsonNode historyState,
                @EventData("trigger") String trigger) {
            super(source, true);
            this.route = route;
            this.query = query;
            this.appShellTitle = appShellTitle;
            this.historyState = historyState;
            this.trigger = trigger;
        }

    }

    /**
     * Event fired by the client to request a refresh of the user interface, by
     * re-navigating to the current route.
     * <p>
     * The route target component is re-instantiated, as well as all layouts in
     * the route chain if the {@code fullRefresh} event flag is active.
     *
     * @see #refreshCurrentRoute(boolean)
     */
    @DomEvent(BrowserRefreshEvent.EVENT_NAME)
    public static class BrowserRefreshEvent extends ComponentEvent<UI> {
        public static final String EVENT_NAME = "ui-refresh";

        private final boolean refreshRouteChain;

        /**
         * Creates a new event instance.
         *
         * @param source
         *            the UI for which the refresh is requested.
         * @param fromClient
         *            <code>true</code> if the event originated from the client
         *            side, <code>false</code> otherwise. NOTE: for technical
         *            reason the argument must be added to the constructor, but
         *            this event the value is always true.
         * @param refreshRouteChain
         *            {@code true} to refresh all layouts in the route chain,
         *            {@code false} to only refresh the route instance
         */
        public BrowserRefreshEvent(UI source, boolean fromClient,
                @EventData("fullRefresh") boolean refreshRouteChain) {
            super(source, true);
            this.refreshRouteChain = refreshRouteChain;
        }
    }

    /**
     * Connect a client with the server side UI. This method is invoked each
     * time client router navigates to a server route.
     *
     * @param event
     *            the event from the browser
     */
    public void browserNavigate(BrowserNavigateEvent event) {

        if (event.appShellTitle != null && !event.appShellTitle.isEmpty()) {
            getInternals().setAppShellTitle(event.appShellTitle);
        }

        final String trimmedRoute = PathUtil.trimPath(event.route);
        final Location location = new Location(trimmedRoute,
                QueryParameters.fromString(event.query));
        NavigationTrigger navigationTrigger;
        if (event.trigger.isEmpty()) {
            navigationTrigger = NavigationTrigger.PAGE_LOAD;
        } else if (event.trigger.equalsIgnoreCase("link")) {
            navigationTrigger = NavigationTrigger.ROUTER_LINK;
        } else if (event.trigger.equalsIgnoreCase("client")) {
            navigationTrigger = NavigationTrigger.CLIENT_SIDE;
        } else {
            navigationTrigger = NavigationTrigger.HISTORY;
        }
        if (firstNavigation) {
            firstNavigation = false;
            getPage().getHistory().setHistoryStateChangeHandler(
                    e -> renderViewForRoute(e.getLocation(), e.getTrigger()));

            if (getInternals().getActiveRouterTargetsChain().isEmpty()) {
                // Render the route unless it was rendered eagerly
                renderViewForRoute(location, navigationTrigger);
            }
        } else {
            History.HistoryStateChangeHandler handler = getPage().getHistory()
                    .getHistoryStateChangeHandler();
            BaseJsonNode state = event.historyState == null ? null
                    : (BaseJsonNode) event.historyState;
            handler.onHistoryStateChange(
                    new History.HistoryStateChangeEvent(getPage().getHistory(),
                            state, location, navigationTrigger));
        }

        // true if the target is client-view and the push mode is disable
        if (getForwardToClientUrl() != null) {
            navigateToClient(getForwardToClientUrl());
            acknowledgeClient();
        } else if (isPostponed()) {
            serverPaused();
        } else {
            // acknowledge client, but cancel if session not open
            serverConnected(
                    !getSession().getState().equals(VaadinSessionState.OPEN));
            replaceStateIfDiffersAndNoReplacePending(event.route, location);
        }
    }

    /**
     * Do a history replaceState if the trimmed route differs from the event
     * route and there is no pending replaceState command.
     *
     * @param route
     *            the event.route
     * @param location
     *            the location with the trimmed route
     */
    private void replaceStateIfDiffersAndNoReplacePending(String route,
            Location location) {
        boolean locationChanged = !location.getPath().equals(route)
                && route.startsWith("/")
                && !location.getPath().equals(route.substring(1));
        boolean containsPendingReplace = !getInternals()
                .containsPendingJavascript("window.history.replaceState")
                && !getInternals().containsPendingJavascript(
                        "'vaadin-navigate', { detail: { state: $0, url: $1, replace: true } }");
        if (locationChanged && containsPendingReplace) {
            // See InternalRedirectHandler invoked via Router.
            getPage().getHistory().replaceState(null, location);
        }
    }

    /**
     * Check that the view can be leave. This method is invoked when the client
     * router tries to navigate to a client route while the current route is a
     * server route.
     *
     * This is only called when client route navigates from a server to a client
     * view.
     *
     * @param event
     *            the event from the browser
     */
    public void leaveNavigation(BrowserLeaveNavigationEvent event) {
        navigateToPlaceholder(new Location(PathUtil.trimPath(event.route),
                QueryParameters.fromString(event.query)));

        // Inform the client whether the navigation should be postponed
        if (isPostponed()) {
            cancelClient();
        } else {
            acknowledgeClient();
        }
    }

    public void navigateToClient(String clientRoute) {
        getPage().executeJs(CLIENT_NAVIGATE_TO, clientRoute);
    }

    private void acknowledgeClient() {
        serverConnected(false);
    }

    private void cancelClient() {
        serverConnected(true);
    }

    private void serverPaused() {
        wrapperElement.executeJs("this.serverPaused()");
    }

    private void serverConnected(boolean cancel) {
        wrapperElement.executeJs(SERVER_CONNECTED, cancel);
    }

    private void navigateToPlaceholder(Location location) {
        if (clientViewNavigationState == null) {
            clientViewNavigationState = new NavigationStateBuilder(
                    getInternals().getRouter())
                    .withTarget(ClientViewPlaceholder.class).build();
        }
        // Passing the `clientViewLocation` to make sure that the navigation
        // events contain the correct location that we are navigating to.
        handleNavigation(location, clientViewNavigationState,
                NavigationTrigger.CLIENT_SIDE);
    }

    private void renderViewForRoute(Location location,
            NavigationTrigger trigger) {
        if (!shouldHandleNavigation(location)) {
            return;
        }
        getInternals().setLastHandledNavigation(location);
        Optional<NavigationState> navigationState = getInternals().getRouter()
                .resolveNavigationTarget(location);
        if (navigationState.isPresent()) {
            // There is a valid route in flow.
            handleNavigation(location, navigationState.get(), trigger);
        } else {
            // When route does not exist, try to navigate to current route
            // in order to check if current view can be left before showing
            // the error page
            navigateToPlaceholder(location);

            if (!isPostponed()) {
                // Route does not exist, and current view does not prevent
                // navigation thus an error page is shown
                handleErrorNavigation(location);
            }

        }
    }

    private boolean shouldHandleNavigation(Location location) {
        return !getInternals().hasLastHandledLocation()
                || !sameLocation(getInternals().getLastHandledLocation(),
                        location);
    }

    private boolean sameLocation(Location oldLocation, Location newLocation) {
        return PathUtil.trimPath(newLocation.getPathWithQueryParameters())
                .equals(PathUtil
                        .trimPath(oldLocation.getPathWithQueryParameters()));
    }

    private void handleNavigation(Location location,
            NavigationState navigationState, NavigationTrigger trigger) {
        try {
            getInternals().setLastHandledNavigation(location);
            NavigationEvent navigationEvent = new NavigationEvent(
                    getInternals().getRouter(), location, this, trigger);

            JavaScriptNavigationStateRenderer clientNavigationStateRenderer = new JavaScriptNavigationStateRenderer(
                    navigationState);

            clientNavigationStateRenderer.handle(navigationEvent);

            forwardToClientUrl = clientNavigationStateRenderer
                    .getClientForwardRoute();

            adjustPageTitle();

        } catch (Exception exception) {
            handleExceptionNavigation(location, exception);
        } finally {
            getInternals().clearLastHandledNavigation();
        }
    }

    private boolean handleExceptionNavigation(Location location,
            Exception exception) {
        Optional<ErrorTargetEntry> maybeLookupResult = getInternals()
                .getRouter().getErrorNavigationTarget(exception);
        if (maybeLookupResult.isPresent()) {
            ErrorTargetEntry lookupResult = maybeLookupResult.get();

            ErrorParameter<?> errorParameter = new ErrorParameter<>(
                    lookupResult.getHandledExceptionType(), exception,
                    exception.getMessage());
            ErrorStateRenderer errorStateRenderer = new ErrorStateRenderer(
                    new NavigationStateBuilder(getInternals().getRouter())
                            .withTarget(lookupResult.getNavigationTarget())
                            .build());

            ErrorNavigationEvent errorNavigationEvent = new ErrorNavigationEvent(
                    getInternals().getRouter(), location, this,
                    NavigationTrigger.CLIENT_SIDE, errorParameter);

            errorStateRenderer.handle(errorNavigationEvent);
        } else {
            throw new RuntimeException(exception);
        }
        return isPostponed();
    }

    private boolean isPostponed() {
        return getInternals().getContinueNavigationAction() != null;
    }

    private void adjustPageTitle() {
        // new title is empty if the flow route does not have a title
        String newTitle = getInternals().getTitle();
        // app shell title is computed from the title tag in index.html
        String appShellTitle = getInternals().getAppShellTitle();
        // restore the app shell title when there is no one for the route
        if ((newTitle == null || newTitle.isEmpty()) && appShellTitle != null
                && !appShellTitle.isEmpty()) {
            getInternals().cancelPendingTitleUpdate();
            getInternals().setTitle(appShellTitle);
        }
    }

    private void handleErrorNavigation(Location location) {
        NavigationState errorNavigationState = getInternals().getRouter()
                .resolveRouteNotFoundNavigationTarget()
                .orElse(getDefaultNavigationError());
        ErrorStateRenderer errorStateRenderer = new ErrorStateRenderer(
                errorNavigationState);
        NotFoundException notFoundException = new NotFoundException(
                "Couldn't find route for '" + location.getPath() + "'");
        ErrorParameter<NotFoundException> errorParameter = new ErrorParameter<>(
                NotFoundException.class, notFoundException);
        ErrorNavigationEvent errorNavigationEvent = new ErrorNavigationEvent(
                getInternals().getRouter(), location, this,
                NavigationTrigger.CLIENT_SIDE, errorParameter);
        errorStateRenderer.handle(errorNavigationEvent);
    }

    private NavigationState getDefaultNavigationError() {
        return new NavigationStateBuilder(getInternals().getRouter())
                .withTarget(RouteNotFoundError.class).build();
    }

    /**
     * Placeholder view when navigating from server-side views to client-side
     * views.
     */
    @Tag(Tag.DIV)
    @AnonymousAllowed
    public static class ClientViewPlaceholder extends Component {
    }
}
