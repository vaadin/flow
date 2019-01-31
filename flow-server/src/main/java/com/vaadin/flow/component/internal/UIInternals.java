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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.internal.ComponentMetaData.DependencyInfo;
import com.vaadin.flow.component.internal.ComponentMetaData.HtmlImportDependency;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.Page.ExecutionCanceler;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.LoadingIndicatorConfigurationMap;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.PollConfigurationMap;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;
import com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.ListenerPriority;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.AfterNavigationHandler;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.router.internal.BeforeLeaveHandler;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Holds UI-specific methods and data which are intended for internal use by the
 * framework.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UIInternals implements Serializable {

    /**
     * A {@link Page#executeJavaScript(String, Serializable...)} invocation that
     * has not yet been sent to the client.
     */
    public static class JavaScriptInvocation implements Serializable {
        private final String expression;
        private final List<Serializable> parameters = new ArrayList<>();

        /**
         * Creates a new invocation.
         *
         * @param expression
         *            the expression to invoke
         * @param parameters
         *            a list of parameters to use when invoking the script
         */
        public JavaScriptInvocation(String expression,
                Serializable... parameters) {
            this.expression = expression;
            Collections.addAll(this.parameters, parameters);
        }

        /**
         * Gets the JavaScript expression to invoke.
         *
         * @return the JavaScript expression
         */
        public String getExpression() {
            return expression;
        }

        /**
         * Gets the parameters to use when invoking the script.
         *
         * @return a list of parameters to use
         */
        public List<Object> getParameters() {
            return Collections.unmodifiableList(parameters);
        }

    }

    /**
     * Tracks which message from the client should come next. First message from
     * the client has id 0.
     */
    private int lastProcessedClientToServerId = -1;

    private int serverSyncId = 0;

    private final StateTree stateTree;

    private PushConnection pushConnection = null;

    /**
     * Timestamp for keeping track of the last heartbeat of the related UI.
     * Updated to the current time whenever the application receives a heartbeat
     * or UIDL request from the client for the related UI.
     */
    private long lastHeartbeatTimestamp = System.currentTimeMillis();

    private List<JavaScriptInvocation> pendingJsInvocations = new ArrayList<>();

    /**
     * The related UI.
     */
    private final UI ui;

    private String title;

    private ExecutionCanceler pendingTitleUpdateCanceler;

    private Location viewLocation = new Location("");
    private ArrayList<HasElement> routerTargetChain = new ArrayList<>();

    private HashMap<Class<?>, List<?>> listeners = new HashMap<>();

    private Location lastHandledNavigation = null;

    private ContinueNavigationAction continueNavigationAction = null;

    /**
     * The Vaadin session to which the related UI belongs.
     */
    private volatile VaadinSession session;

    private final DependencyList dependencyList = new DependencyList();

    private final ConstantPool constantPool = new ConstantPool();

    private AbstractTheme theme = null;

    private static final Pattern componentSource = Pattern
            .compile(".*/src/vaadin-([\\w\\-]*).html");

    private byte[] lastProcessedMessageHash = null;

    private String contextRootRelativePath;

    private String appId;

    /**
     * Creates a new instance for the given UI.
     *
     * @param ui
     *            the UI to use
     */
    public UIInternals(UI ui) {
        this.ui = ui;
        stateTree = new StateTree(this, getRootNodeFeatures());
    }

    /**
     * Gets the state tree of the related UI.
     *
     * @return the state tree
     */
    public StateTree getStateTree() {
        return stateTree;
    }

    /**
     * Gets the last processed server message id.
     * <p>
     * Used internally for communication tracking.
     *
     * @return lastProcessedServerMessageId the id of the last processed server
     *         message
     */
    public int getLastProcessedClientToServerId() {
        return lastProcessedClientToServerId;
    }

    /**
     * Gets the hash of the last processed message from the client.
     * <p>
     * The hash is set through
     * {@link #setLastProcessedClientToServerId(int, byte[])}.
     * <p>
     * Used internally for communication tracking.
     *
     * @return the hash as a byte array, or <code>null</code> if no hash has
     *         been set
     */
    public byte[] getLastProcessedMessageHash() {
        return lastProcessedMessageHash;
    }

    /**
     * Sets the last processed server message id.
     * <p>
     * Used internally for communication tracking.
     *
     * @param lastProcessedClientToServerId
     *            the id of the last processed server message
     * @param lastProcessedMessageHash
     *            the hash of the message
     */
    public void setLastProcessedClientToServerId(
            int lastProcessedClientToServerId,
            byte[] lastProcessedMessageHash) {
        this.lastProcessedClientToServerId = lastProcessedClientToServerId;
        this.lastProcessedMessageHash = lastProcessedMessageHash;
    }

    /**
     * Gets the server sync id.
     * <p>
     * The sync id is incremented by one whenever a new response is written.
     * This id is then sent over to the client. The client then adds the most
     * recent sync id to each communication packet it sends back to the server.
     * This way, the server knows at what state the client is when the packet is
     * sent. If the state has changed on the server side since that, the server
     * can try to adjust the way it handles the actions from the client side.
     * <p>
     * The sync id value <code>-1</code> is ignored to facilitate testing with
     * pre-recorded requests.
     *
     * @return the server sync id
     */
    public int getServerSyncId() {
        return serverSyncId;
    }

    /**
     * Increments the server sync id.
     * <p>
     * This should only be called by whoever sends a message to the client,
     * after the message has been sent.
     */
    public void incrementServerId() {
        serverSyncId++;
    }

    /**
     * Returns the timestamp of the last received heartbeat for the related UI.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return the UI.
     *
     * @see VaadinService#closeInactiveUIs(VaadinSession)
     *
     * @return The time the last heartbeat request occurred, in milliseconds
     *         since the epoch.
     */
    public long getLastHeartbeatTimestamp() {
        return lastHeartbeatTimestamp;
    }

    /**
     * Sets the last heartbeat request timestamp for the related UI. Called by
     * the framework whenever the application receives a valid heartbeat request
     * for the UI.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return the UI.
     *
     * @param lastHeartbeat
     *            The time the last heartbeat request occurred, in milliseconds
     *            since the epoch.
     */
    public void setLastHeartbeatTimestamp(long lastHeartbeat) {
        lastHeartbeatTimestamp = lastHeartbeat;
        HeartbeatEvent heartbeatEvent = new HeartbeatEvent(ui, lastHeartbeat);
        getListeners(HeartbeatListener.class)
                .forEach(listener -> listener.heartbeat(heartbeatEvent));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends NodeFeature>[] getRootNodeFeatures() {
        // Start with all element features
        List<Class<? extends NodeFeature>> features = new ArrayList<>(
                BasicElementStateProvider.getFeatures());

        // Then add our own custom features
        features.add(PushConfigurationMap.class);
        features.add(PollConfigurationMap.class);
        features.add(ReconnectDialogConfigurationMap.class);
        features.add(LoadingIndicatorConfigurationMap.class);

        // And return them all
        assert features.size() == new HashSet<>(features)
                .size() : "There are duplicates";
        return (Class<? extends NodeFeature>[]) features
                .toArray(new Class<?>[0]);
    }

    private static String getSessionDetails(VaadinSession session) {
        if (session == null) {
            return null;
        } else {
            return session + " for " + session.getService().getServiceName();
        }
    }

    /**
     * Sets the session to which the related UI is assigned.
     * <p>
     * This method is for internal use by the framework. To explicitly close a
     * UI, see {@link UI#close()}.
     *
     * @param session
     *            the session to set
     *
     * @throws IllegalStateException
     *             if the session has already been set
     *
     * @see #getSession()
     */
    public void setSession(VaadinSession session) {
        if (session == null && this.session == null) {
            throw new IllegalStateException(
                    "Session should never be set to null when UI.session is already null");
        } else if (session != null && this.session != null) {
            throw new IllegalStateException(
                    "Session has already been set. Old session: "
                            + getSessionDetails(this.session)
                            + ". New session: " + getSessionDetails(session)
                            + ".");
        } else {
            if (session == null) {
                try {
                    ComponentUtil.onComponentDetach(ui);
                    ui.getChildren().forEach(ComponentUtil::onComponentDetach);
                } catch (Exception e) {
                    getLogger().warn("Error while detaching UI from session",
                            e);
                }
                // Disable push when the UI is detached. Otherwise the
                // push connection and possibly VaadinSession will live on.
                ui.getPushConfiguration().setPushMode(PushMode.DISABLED);
                setPushConnection(null);
            }
            this.session = session;
        }

        if (session != null) {
            ComponentUtil.onComponentAttach(ui, true);
        }
    }

    /**
     * Returns the internal push connection object used by the related UI. This
     * method should only be called by the framework.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return the UI.
     *
     * @return the push connection used by the UI, or {@code null} if push is
     *         not available.
     */
    public PushConnection getPushConnection() {
        assert !(ui.getPushConfiguration().getPushMode().isEnabled()
                && pushConnection == null);
        return pushConnection;
    }

    /**
     * Sets the internal push connection object used by the related UI. This
     * method should only be called by the framework.
     * <p>
     * The {@code pushConnection} argument must be non-null if and only if
     * {@code getPushConfiguration().getPushMode().isEnabled()}.
     *
     * @param pushConnection
     *            the push connection to use for the UI
     */
    public void setPushConnection(PushConnection pushConnection) {
        // If pushMode is disabled then there should never be a pushConnection;
        // if enabled there should always be
        assert (pushConnection == null)
                ^ ui.getPushConfiguration().getPushMode().isEnabled();

        if (pushConnection == this.pushConnection) {
            return;
        }

        if (this.pushConnection != null && this.pushConnection.isConnected()) {
            this.pushConnection.disconnect();
        }

        this.pushConnection = pushConnection;
    }

    /**
     * Add a listener that will be informed when a new set of components are
     * going to be attached.
     *
     * @param listener
     *            the before enter listener
     * @return handler to remove the event listener
     */
    public Registration addBeforeEnterListener(BeforeEnterListener listener) {
        return addListener(BeforeEnterHandler.class, listener);
    }

    /**
     * Add a listener that will be informed when old components are detached.
     *
     * @param listener
     *            the before leave listener
     * @return handler to remove the event listener
     */
    public Registration addBeforeLeaveListener(BeforeLeaveListener listener) {
        return addListener(BeforeLeaveHandler.class, listener);
    }

    /**
     * Add a listener that will be informed when new components have been
     * attached and all navigation tasks have resolved.
     *
     * @param listener
     *            the after navigation listener
     * @return handler to remove the event listener
     */
    public Registration addAfterNavigationListener(
            AfterNavigationListener listener) {
        return addListener(AfterNavigationHandler.class, listener);
    }

    public Registration addHeartbeatListener(HeartbeatListener listener) {
        return addListener(HeartbeatListener.class, listener);
    }

    private <E> Registration addListener(Class<E> handler, E listener) {
        session.checkHasLock();
        List<E> list = (List<E>) listeners
                .computeIfAbsent(handler, key -> new ArrayList<>());
        list.add(listener);

        list.sort((o1, o2) -> {
            Class<?> o1Class = o1.getClass();
            Class<?> o2Class = o2.getClass();

            final ListenerPriority listenerPriority1 = o1Class
                    .getAnnotation(ListenerPriority.class);
            final ListenerPriority listenerPriority2 = o2Class
                    .getAnnotation(ListenerPriority.class);

            final int priority1 = listenerPriority1 != null
                    ? listenerPriority1.value() : 0;
            final int priority2 = listenerPriority2 != null
                    ? listenerPriority2.value() : 0;

            // we want to have a descending order
            return Integer.compare(priority2, priority1);
        });

        return () -> list.remove(listener);
    }

    /**
     * Get all registered listeners for given navigation handler type.
     *
     * @param handler
     *         handler to get listeners for
     * @param <E>
     *         the handler type
     * @return unmodifiable list of registered listeners for navigation handler
     */
    public <E> List<E> getListeners(Class<E> handler) {
        List<E> registeredListeners = (List<E>) listeners
                .computeIfAbsent(handler, key -> new ArrayList<>());

        return Collections.unmodifiableList(registeredListeners);
    }

    /**
     * Adds a JavaScript invocation to be sent to the client.
     *
     * @param invocation
     *            the invocation to add
     * @return a callback for canceling the execution if not yet sent to browser
     */
    public ExecutionCanceler addJavaScriptInvocation(
            JavaScriptInvocation invocation) {
        session.checkHasLock();
        pendingJsInvocations.add(invocation);
        return () -> pendingJsInvocations.remove(invocation);
    }

    /**
     * Gets all the pending JavaScript invocations and clears the queue.
     *
     * @return a list of pending JavaScript invocations
     */
    public List<JavaScriptInvocation> dumpPendingJavaScriptInvocations() {
        pendingTitleUpdateCanceler = null;

        if (pendingJsInvocations.isEmpty()) {
            return Collections.emptyList();
        }

        List<JavaScriptInvocation> currentList = pendingJsInvocations;

        pendingJsInvocations = new ArrayList<>();

        return currentList;
    }

    /**
     * Gets the pending javascript invocations added with
     * {@link #addJavaScriptInvocation(JavaScriptInvocation)} after last
     * {@link #dumpPendingJavaScriptInvocations()}.
     *
     * @return the pending javascript invocations, never <code>null</code>
     */
    // Non-private for testing purposes
    List<JavaScriptInvocation> getPendingJavaScriptInvocations() {
        return pendingJsInvocations;
    }

    /**
     * Records the page title set with {@link Page#setTitle(String)}.
     * <p>
     * You should not set the page title for the browser with this method, use
     * {@link Page#setTitle(String)} instead.
     *
     * @param title
     *            the title to set
     */
    public void setTitle(String title) {
        assert title != null;
        JavaScriptInvocation invocation = new JavaScriptInvocation(
                "document.title = $0", title);

        pendingTitleUpdateCanceler = addJavaScriptInvocation(invocation);

        this.title = title;
    }

    /**
     * Gets the page title recorded with {@link Page#setTitle(String)}.
     * <p>
     * <b>NOTE</b> this might not be up to date with the actual title set since
     * it is not updated from the browser and the update might have been
     * canceled before it has been sent to the browser with
     * {@link #cancelPendingTitleUpdate()}.
     *
     * @return the page title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Cancels any pending page title update set via {@link #setTitle(String)}.
     *
     * @return <code>true</code> if pending title update was cancelled,
     *         <code>false</code> if not
     */
    public boolean cancelPendingTitleUpdate() {
        if (pendingTitleUpdateCanceler == null) {
            return false;
        }

        boolean result = pendingTitleUpdateCanceler.cancelExecution();
        pendingTitleUpdateCanceler = null;
        return result;
    }

    /**
     * Shows a route target in the related UI. This method is intended for
     * framework use only. Use {@link UI#navigate(String)} to change the route
     * target that is shown in a UI.
     *
     * @param viewLocation
     *            the location of the route target relative to the servlet
     *            serving the UI, not <code>null</code>
     * @param target
     *            the component to show, not <code>null</code>
     * @param path
     *            the resolved route path so we can determine what the rendered
     *            target is for
     * @param layouts
     *            the parent layouts
     */
    public void showRouteTarget(Location viewLocation, String path,
            Component target, List<RouterLayout> layouts) {
        assert target != null;
        assert viewLocation != null;

        updateTheme(target, path);

        HasElement oldRoot = null;
        if (!routerTargetChain.isEmpty()) {
            oldRoot = routerTargetChain.get(routerTargetChain.size() - 1);
        }

        this.viewLocation = viewLocation;

        Element uiElement = ui.getElement();

        // Assemble previous parent-child relationships to enable detecting
        // changes
        Map<RouterLayout, HasElement> oldChildren = new HashMap<>();
        for (int i = 0; i < routerTargetChain.size() - 1; i++) {
            HasElement child = routerTargetChain.get(i);
            RouterLayout parent = (RouterLayout) routerTargetChain.get(i + 1);

            oldChildren.put(parent, child);
        }

        routerTargetChain = new ArrayList<>();
        routerTargetChain.add(target);

        if (layouts != null) {
            routerTargetChain.addAll(layouts);
        }

        // Ensure the entire chain is connected
        HasElement root = null;
        for (HasElement part : routerTargetChain) {
            if (root != null) {
                assert part instanceof RouterLayout : "All parts of the chain except the first must implement "
                        + RouterLayout.class.getSimpleName();
                RouterLayout parent = (RouterLayout) part;
                HasElement oldChild = oldChildren.get(parent);
                if (oldChild != root) {
                    removeFromParent(oldChild);
                    parent.showRouterLayoutContent(root);
                }
            } else if (part instanceof RouterLayout
                    && oldChildren.containsKey(part)) {
                // Remove old child view from leaf view if it had one
                removeFromParent(oldChildren.get(part));
                ((RouterLayout) part).showRouterLayoutContent(null);
            }
            root = part;
        }

        if (root == null) {
            throw new IllegalArgumentException(
                    "Root can't be null here since we know there's at least one item in the chain");
        }

        Element rootElement = root.getElement();

        if (!uiElement.equals(rootElement.getParent())) {
            if (oldRoot != null) {
                oldRoot.getElement().removeFromParent();
            }
            rootElement.removeFromParent();
            uiElement.appendChild(rootElement);
        }
    }

    private void updateTheme(Component target, String path) {
        Optional<ThemeDefinition> themeDefinition = ui
                .getThemeFor(target.getClass(), path);

        if (themeDefinition.isPresent()) {
            Class<? extends AbstractTheme> themeClass = themeDefinition.get()
                    .getTheme();
            if (theme == null || !theme.getClass().equals(themeClass)) {
                theme = ReflectTools.createInstance(themeClass);
            }
        } else {
            theme = null;
            if (!AnnotationReader
                    .getAnnotationFor(target.getClass(), NoTheme.class)
                    .isPresent()) {
                getLogger().warn(
                        "No @Theme defined for {}. See 'trace' level logs for the exact components missing theming.",
                        target.getClass().getName());
            }
        }
    }

    /**
     * Set the Theme to use for HTML import theme translations.
     * <p>
     * Note! The set theme will be overridden for each call to {@link
     * #showRouteTarget(Location, String, Component, List)} if the new theme is
     * not the same as the set theme.
     * <p>
     * This method is intended for managed internal use only.
     *
     * @param theme theme implementation to set
     */
    public void setTheme(AbstractTheme theme) {
        this.theme = theme;
    }

    private void removeFromParent(HasElement component) {
        if (component != null) {
            component.getElement().removeFromParent();
        }
    }

    /**
     * Gets the currently active router target and parent layouts.
     *
     * @return a list of active router target and parent layout instances,
     *         starting from the innermost part
     */
    public List<HasElement> getActiveRouterTargetsChain() {
        return Collections.unmodifiableList(routerTargetChain);
    }

    /**
     * Gets the location of the currently shown view. The location is relative
     * the servlet mapping used for serving the related UI.
     *
     * @return the view location, not <code>null</code>
     */
    public Location getActiveViewLocation() {
        return viewLocation;
    }

    /**
     * Gets the VaadinSession to which the related UI is attached.
     *
     * <p>
     * The method will return {@code null} if the UI is not currently attached
     * to a VaadinSession.
     * </p>
     *
     * @return the VaadinSession to which the related UI is attached
     */
    public VaadinSession getSession() {
        return session;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(UIInternals.class.getName());
    }

    /**
     * Returns the helper which handles loading of dependencies (css, js).
     *
     * @return the dependency list helper
     */
    public DependencyList getDependencyList() {
        return dependencyList;
    }

    /**
     * Adds the dependencies defined using {@link StyleSheet},
     * {@link JavaScript} or {@link HtmlImport} on the given Component class.
     *
     * @param componentClass
     *            the component class to read annotations from
     */
    public void addComponentDependencies(
            Class<? extends Component> componentClass) {
        Page page = ui.getPage();
        DependencyInfo dependencies = ComponentUtil
                .getDependencies(session.getService(), componentClass);
        dependencies.getHtmlImports()
                .forEach(html -> addHtmlImport(html, page));
        dependencies.getJavaScripts()
                .forEach(js -> page.addJavaScript(js.value(), js.loadMode()));
        dependencies.getStyleSheets().forEach(styleSheet -> page
                .addStyleSheet(styleSheet.value(), styleSheet.loadMode()));
    }

    private void addHtmlImport(HtmlImportDependency dependency, Page page) {
        // The HTML dependency parser does not consider themes so it can
        // cache raw information (e.g. vaadin-button/src/vaadin-button.html
        // import) and not take into consideration which themes might
        // contain versions for which files. They must be translated before
        // added to page though, as whatever is added there is sent without
        // modifications to the client
        dependency.getUris().forEach(uri -> page
                .addHtmlImport(translateTheme(uri), dependency.getLoadMode()));
    }

    private String translateTheme(String importValue) {
        if (theme != null) {
            VaadinService service = session.getService();
            WebBrowser browser = session.getBrowser();
            Optional<String> themedUrl = service.getThemedUrl(importValue,
                    browser, theme);
            return themedUrl.orElse(importValue);
        } else {
            Matcher componentMatcher = componentSource.matcher(importValue);
            if (componentMatcher.matches()) {
                String componentName = componentMatcher.group(1);
                getLogger().trace(
                        "Vaadin component '{}' is used and missing theme definition.",
                        componentName);
            }
        }
        return importValue;
    }

    /**
     * Gets the constant pool that is used for keeping track of constants shared
     * with the client for this UI.
     *
     * @return the constant pool to use, not <code>null</code>
     */
    public ConstantPool getConstantPool() {
        return constantPool;
    }

    /**
     * Get the latest handled location or empty optional if no active
     * navigation.
     *
     * @return location if navigated during active navigation or {@code null}
     */
    public Location getLastHandledLocation() {
        return lastHandledNavigation;
    }

    /**
     * Set the latest navigation location for active navigation.
     *
     * @param location
     *            last location navigated to
     */
    public void setLastHandledNavigation(Location location) {
        lastHandledNavigation = location;
    }

    /**
     * Check if we have already started navigation to some location on this
     * roundtrip.
     *
     * @return true if the last navigation location {@code !=} null
     */
    public boolean hasLastHandledLocation() {
        return lastHandledNavigation != null;
    }

    /**
     * Clear latest handled navigation location.
     */
    public void clearLastHandledNavigation() {
        setLastHandledNavigation(null);
    }

    /**
     * Get stored {@link ContinueNavigationAction} if any.
     *
     * @return continue navigation action object
     */
    public ContinueNavigationAction getContinueNavigationAction() {
        return continueNavigationAction;
    }

    /**
     * Set a {@link ContinueNavigationAction} or null to clear existing action.
     *
     * @param continueNavigationAction
     *            continue navigatio action to store or null
     */
    public void setContinueNavigationAction(
            ContinueNavigationAction continueNavigationAction) {
        this.continueNavigationAction = continueNavigationAction;
    }

    /**
     * Sets the application id tied with this UI. Different applications in the
     * same page have different unique ids.
     *
     * @param appId
     *            the id of the application tied with this UI
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Gets the application id tied with this UI. Different applications in the
     * same page have different unique ids.
     *
     * @return the id of the application tied with this UI
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Gets the router used for navigating in this UI, if the router was active
     * when this UI was initialized.
     *
     * @return the router used for this UI, or <code>null</code> if there is no
     *         router
     */
    public Router getRouter() {
        return getSession().getService().getRouter();
    }

    /**
     * Checks if there are changes waiting to be sent to the client side.
     *
     * @return <code>true</code> if there are pending changes,
     *         <code>false</code> otherwise
     */
    public boolean isDirty() {
        return getStateTree().isDirty()
                || !getPendingJavaScriptInvocations().isEmpty();
    }

    /**
     * Sets the relative path from the UI (servlet) path to the context root.
     *
     * @param contextRootRelativePath
     *            the relative path from servlet to context root
     */
    public void setContextRoot(String contextRootRelativePath) {
        this.contextRootRelativePath = contextRootRelativePath;
    }

    /**
     * Gets the relative path from the UI (servlet) path to the context root.
     *
     * @return the relative path from servlet to context root
     */
    public String getContextRootRelativePath() {
        return contextRootRelativePath;
    }

    /**
     * Gets the UI that this instance belongs to.
     *
     * @return the UI instance.
     */
    public UI getUI() {
        return ui;
    }
}
