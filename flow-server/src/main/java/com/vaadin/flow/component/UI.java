/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree.ExecutionRegistration;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.LoadingIndicatorConfigurationMap;
import com.vaadin.flow.internal.nodefeature.PollConfigurationMap;
import com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.EventUtil;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandlingCommand;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.startup.RouteRegistry;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

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
 * @since 7.0
 */
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

    private final UIInternals internals = new UIInternals(this);

    private final Page page = new Page(this);

    /**
     * Creates a new empty UI.
     */
    public UI() {
        super(null);
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
     * </p>
     *
     * <p>
     * Getting a null value is often a problem in constructors of regular
     * components and in the initializers of custom composite components. A
     * standard workaround is to use {@link VaadinSession#getCurrent()} to
     * retrieve the application instance that the current request relates to.
     * Another way is to move the problematic initialization to
     * {@link #onAttach(AttachEvent)}, as described in the documentation of the
     * method.
     * </p>
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
     *
     * @param request
     *            the initialization request
     * @param uiId
     *            the id of the new ui
     *
     * @see #getUIId()
     */
    public void doInit(VaadinRequest request, int uiId) {
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

        String appId = getSession().getService().getMainDivId(getSession(),
                request);
        appId = appId.substring(0, appId.indexOf('-'));
        getInternals().setAppId(appId);

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
     * <li>after three missed heartbeat requests.
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
     * @since 7.1
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
        Map<Class<?>, CurrentInstance> old = null;

        VaadinSession session = getSession();

        if (session == null) {
            throw new UIDetachedException();
        }

        VaadinService.verifyNoOtherSessionLocked(session);

        session.lock();
        try {
            if (getSession() == null) {
                // UI was detached after fetching the session but before we
                // acquired the lock.
                throw new UIDetachedException();
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
     * </p>
     * <p>
     * RPC handlers for components inside this UI do not need to use this method
     * as the session is automatically locked by the framework during RPC
     * handling.
     * </p>
     * <p>
     * Please note that the command might be invoked on a different thread or
     * later on the current thread, which means that custom thread locals might
     * not have the expected values when the command is executed.
     * {@link UI#getCurrent()}, {@link VaadinSession#getCurrent()} and
     * {@link VaadinService#getCurrent()} are set according to this UI before
     * executing the command. Other standard CurrentInstance values such as
     * {@link VaadinService#getCurrentRequest()} and
     * {@link VaadinService#getCurrentResponse()} will not be defined.
     * </p>
     * <p>
     * The returned future can be used to check for task completion and to
     * cancel the task.
     * </p>
     *
     * @see #getCurrent()
     * @see #accessSynchronously(Command)
     * @see VaadinSession#access(Command)
     * @see VaadinSession#lock()
     *
     * @since 7.1
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
        VaadinSession session = getSession();

        if (session == null) {
            throw new UIDetachedException();
        }

        return session.access(new ErrorHandlingCommand() {
            @Override
            public void execute() {
                accessSynchronously(command);
            }

            @Override
            public void handleError(Exception exception) {
                try {
                    if (command instanceof ErrorHandlingCommand) {
                        ErrorHandlingCommand errorHandlingCommand = (ErrorHandlingCommand) command;
                        errorHandlingCommand.handleError(exception);
                    } else {
                        getSession().getErrorHandler()
                                .error(new ErrorEvent(exception));
                    }
                } catch (Exception e) {
                    getLogger().error(e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Sets the interval with which the UI should poll the server to see if
     * there are any changes. Polling is disabled by default.
     * <p>
     * Note that it is possible to enable push and polling at the same time but
     * it should not be done to avoid excessive server traffic.
     * </p>
     * <p>
     * Add-on developers should note that this method is only meant for the
     * application developer. An add-on should not set the poll interval
     * directly, rather instruct the user to set it.
     * </p>
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
     * @since 7.1
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

        if (!getInternals().isDirty()) {
            // Do not push if there is nothing to push
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
     * @since 7.1
     * @return The instance used for push configuration
     */
    public PushConfiguration getPushConfiguration() {
        return pushConfiguration;
    }

    /**
     * Retrieves the object used for configuring the reconnect dialog.
     *
     * @since 7.6
     * @return The instance used for reconnect dialog configuration
     */
    public ReconnectDialogConfiguration getReconnectDialogConfiguration() {
        return getNode().getFeature(ReconnectDialogConfigurationMap.class);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(UI.class.getName());
    }

    /**
     * * Gets the locale for this UI.
     *
     * @return the locale in use
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale for this UI.
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
     * Gets the {@link ThemeDefinition} associated with the given navigation
     * target, if any. The theme is defined by using the {@link Theme}
     * annotation on the navigation target class.
     * <p>
     * If no {@link Theme} and {@link NoTheme} annotation are used, by default
     * the {@code com.vaadin.flow.theme.lumo.Lumo} class is used (if present on
     * the classpath).
     *
     * @param navigationTarget
     *            the navigation target class
     * @param path
     *            the resolved route path so we can determine what the rendered
     *            target is for
     * @return the associated ThemeDefinition, or empty if none is defined and
     *         the Lumo class is not in the classpath, or if the NoTheme
     *         annotation is being used.
     * @see RouteRegistry#getThemeFor(Class, String)
     */
    public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path) {
        return getRouter().getRegistry().getThemeFor(navigationTarget, path);
    }

    /**
     * Updates this UI to show the view corresponding to the given navigation
     * target.
     * <p>
     * Besides the navigation to the {@code location} this method also updates
     * the browser location (and page history).
     * 
     * @param navigationTarget
     *            navigation target to navigate to
     */
    public void navigate(Class<? extends Component> navigationTarget) {
        String routeUrl = getRouter().getUrl(navigationTarget);
        navigate(routeUrl);
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
     * 
     * @param navigationTarget
     *            navigation target to navigate to
     * @param parameter
     *            parameter to pass to view
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> void navigate(
            Class<? extends C> navigationTarget, T parameter) {
        String routeUrl = getRouter().getUrl(navigationTarget, parameter);
        navigate(routeUrl);
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
     * @param location
     *            the location to navigate to, not {@code null}
     * @param queryParameters
     *            query parameters that are used for navigation, not
     *            {@code null}
     */
    public void navigate(String location, QueryParameters queryParameters) {
        if (location == null) {
            throw new IllegalArgumentException("Location may not be null");
        }
        if (queryParameters == null) {
            throw new IllegalArgumentException(
                    "Query parameters may not be null");
        }

        Location navigationLocation = new Location(location, queryParameters);
        if (!internals.hasLastHandledLocation()
                || !navigationLocation.getPathWithQueryParameters()
                        .equals(internals.getLastHandledLocation()
                                .getPathWithQueryParameters())) {
            // Enable navigating back
            getPage().getHistory().pushState(null, navigationLocation);
        }
        getRouter().navigate(this, navigationLocation,
                NavigationTrigger.PROGRAMMATIC);
    }

    /**
     * Gets the router used for navigating in this UI.
     *
     * @return a router
     */
    public Router getRouter() {
        return internals.getRouter();
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
     */
    public ExecutionRegistration beforeClientResponse(Component component,
            SerializableConsumer<ExecutionContext> execution) {

        if (component == null) {
            throw new IllegalArgumentException(
                    "The 'component' parameter may not be null");
        }
        if (execution == null) {
            throw new IllegalArgumentException(
                    "The 'execution' parameter may not be null");
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
     * Get all registered listener of given navigation handler type.
     *
     * @param navigationHandler
     *            navigation handler type to get listeners for
     * @param <E>
     *            the handler type
     * @return unmodifiable list of registered listeners for navigation handler
     */
    public <E> List<E> getNavigationListeners(Class<E> navigationHandler) {
        return internals.getNavigationListeners(navigationHandler);
    }
}
