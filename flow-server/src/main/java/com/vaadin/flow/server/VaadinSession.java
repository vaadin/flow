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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Contains everything that Vaadin needs to store for a specific user. This is
 * typically stored in a {@link HttpSession}, but others storage mechanisms
 * might also be used.
 * <p>
 * Everything inside a {@link VaadinSession} should be serializable to ensure
 * compatibility with schemes using serialization for persisting the session
 * data.
 * <p>
 * Current VaadinSession object which can be accessed by
 * {@link VaadinSession#getCurrent} is not present before {@link VaadinServlet}
 * starts handling the HTTP request. For example, it cannot be used in any
 * implementation of {@link javax.servlet.Filter} interface.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class VaadinSession implements HttpSessionBindingListener, Serializable {

    private static final String SESSION_NOT_LOCKED_MESSAGE = "Cannot access state in VaadinSession or UI without locking the session.";

    /**
     * The name of the parameter that is by default used in e.g. web.xml to
     * define the name of the default {@link UI} class.
     */
    // javadoc in UI should be updated if this value is changed
    public static final String UI_PARAMETER = "UI";

    /**
     * Configuration for the session.
     */
    private DeploymentConfiguration configuration;

    /**
     * Default locale of the session.
     */
    private Locale locale = Locale.getDefault();

    /**
     * Session wide error handler which is used by default if an error is left
     * unhandled.
     */
    private ErrorHandler errorHandler = new DefaultErrorHandler();
    private LinkedList<RequestHandler> requestHandlers = new LinkedList<>();

    private int nextUIId = 0;
    private Map<Integer, UI> uIs = new HashMap<>();

    protected WebBrowser browser = new WebBrowser();

    private long cumulativeRequestDuration = 0;

    private long lastRequestDuration = -1;

    private long lastRequestTimestamp = System.currentTimeMillis();

    private VaadinSessionState state = VaadinSessionState.OPEN;

    private transient WrappedSession session;

    private transient VaadinService service;

    private transient Lock lock;

    /*
     * Pending tasks can't be serialized and the queue should be empty when the
     * session is serialized as long as it doesn't happen while some other
     * thread has the lock.
     */
    private transient ConcurrentLinkedQueue<FutureAccess> pendingAccessQueue = new ConcurrentLinkedQueue<>();

    private final String pushId = UUID.randomUUID().toString();

    private final Attributes attributes = new Attributes();

    private final StreamResourceRegistry resourceRegistry;

    /**
     * Creates a new VaadinSession tied to a VaadinService.
     *
     * @param service
     *            the Vaadin service for the new session
     */
    public VaadinSession(VaadinService service) {
        this.service = service;
        resourceRegistry = createStreamResourceRegistry();
    }

    /**
     * Creates the StreamResourceRegistry for this session.
     *
     * @return A StreamResourceRegistry instance
     */
    protected StreamResourceRegistry createStreamResourceRegistry() {
        return new StreamResourceRegistry(this);
    }

    /**
     * @see javax.servlet.http.HttpSessionBindingListener#valueBound(HttpSessionBindingEvent)
     */
    @Override
    public void valueBound(HttpSessionBindingEvent arg0) {
        // We are not interested in bindings
    }

    /**
     * @see javax.servlet.http.HttpSessionBindingListener#valueUnbound(HttpSessionBindingEvent)
     */
    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        // If we are going to be unbound from the session, the session must be
        // closing
        // Notify the service
        if (service == null) {
            getLogger()
                    .warn("A VaadinSession instance not associated to any service is getting unbound. "
                            + "Session destroy events will not be fired and UIs in the session will not get detached. "
                            + "This might happen if a session is deserialized but never used before it expires.");
        } else if (VaadinService.getCurrentRequest() != null
                && getCurrent() == this) {
            checkHasLock();
            // Ignore if the session is being moved to a different backing
            // session or if GAEVaadinServlet is doing its normal cleanup.
            if (getAttribute(
                    VaadinService.PRESERVE_UNBOUND_SESSION_ATTRIBUTE) == Boolean.TRUE) {
                return;
            }

            // There is still a request in progress for this session. The
            // session will be destroyed after the response has been written.
            if (getState() == VaadinSessionState.OPEN) {
                close();
            }
        } else {
            // We are not in a request related to this session so we can destroy
            // it as soon as we acquire the lock.
            service.fireSessionDestroy(this);
        }
        session = null;
    }

    /**
     * Get the web browser associated with this session.
     *
     * @return the web browser object
     */
    public WebBrowser getBrowser() {
        checkHasLock();
        return browser;
    }

    /**
     * @return The total time spent servicing requests in this session, in
     *         milliseconds.
     */
    public long getCumulativeRequestDuration() {
        checkHasLock();
        return cumulativeRequestDuration;
    }

    /**
     * Sets the time spent servicing the last request in the session and updates
     * the total time spent servicing requests in this session.
     *
     * @param time
     *            The time spent in the last request, in milliseconds.
     */
    public void setLastRequestDuration(long time) {
        checkHasLock();
        lastRequestDuration = time;
        cumulativeRequestDuration += time;
    }

    /**
     * @return The time spent servicing the last request in this session, in
     *         milliseconds.
     */
    public long getLastRequestDuration() {
        checkHasLock();
        return lastRequestDuration;
    }

    /**
     * Sets the time when the last UIDL request was serviced in this session.
     *
     * @param timestamp
     *            The time when the last request was handled, in milliseconds
     *            since the epoch.
     */
    public void setLastRequestTimestamp(long timestamp) {
        checkHasLock();
        lastRequestTimestamp = timestamp;
    }

    /**
     * Returns the time when the last request was serviced in this session.
     *
     * @return The time when the last request was handled, in milliseconds since
     *         the epoch.
     */
    public long getLastRequestTimestamp() {
        checkHasLock();
        return lastRequestTimestamp;
    }

    /**
     * Gets the underlying session to which this service session is currently
     * associated.
     *
     * @return the wrapped session for this context
     */
    public WrappedSession getSession() {
        /*
         * This is used to fetch the underlying session and there is no need for
         * having a lock when doing this. On the contrary this is sometimes done
         * to be able to lock the session.
         */
        return session;
    }

    /**
     * Retrieves all {@link VaadinSession}s which are stored in the given HTTP
     * session.
     *
     * @param httpSession
     *            the HTTP session
     * @return the found VaadinSessions
     */
    public static Collection<VaadinSession> getAllSessions(
            HttpSession httpSession) {
        Set<VaadinSession> sessions = new HashSet<>();
        Enumeration<String> attributeNames = httpSession.getAttributeNames();

        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            if (attributeName.startsWith(VaadinSession.class.getName() + ".")) {
                Object value = httpSession.getAttribute(attributeName);
                if (value instanceof VaadinSession) {
                    sessions.add((VaadinSession) value);
                }
            }
        }
        return sessions;
    }

    /**
     * Updates the transient session lock from VaadinService.
     */
    private void refreshLock() {
        assert lock == null || lock == service.getSessionLock(
                session) : "Cannot change the lock from one instance to another";
        assert hasLock(service, session);
        lock = service.getSessionLock(session);
    }

    public void setConfiguration(DeploymentConfiguration configuration) {
        checkHasLock();
        if (configuration == null) {
            throw new IllegalArgumentException("Can not set to null");
        }
        assert this.configuration == null : "Configuration can only be set once";
        this.configuration = configuration;
    }

    /**
     * Gets the configuration for this session.
     *
     * @return the deployment configuration
     */
    public DeploymentConfiguration getConfiguration() {
        checkHasLock();
        return configuration;
    }

    /**
     * Gets the locale for this session.
     * <p>
     * The default locale is determined in different ways depending on whether a
     * {@link I18NProvider} is available.
     * <p>
     * If a i18n provider is available, the locale is determined by selecting
     * the locale from {@link I18NProvider#getProvidedLocales()} that best
     * matches the user agent preferences (i.e. the <code>Accept-Language</code>
     * header). If an exact match is found, then that locale is used. Otherwise,
     * the matching logic looks for the first provided locale that uses the same
     * language regardless of the country. If no other match is found, then the
     * first item from {@link I18NProvider#getProvidedLocales()} is used.
     * <p>
     * If no i18n provider is available, then the {@link Locale#getDefault()
     * default JVM locale} is used as the default locale.
     *
     * @return the locale of this session.
     */
    public Locale getLocale() {
        checkHasLock();
        return locale;
    }

    /**
     * Sets the default locale for this session.
     * <p>
     * Setting the locale of a session will also override any custom locale
     * configured for all UIs in this session.
     *
     * @param locale
     *            the locale to set, not <code>null</code>
     */
    public void setLocale(Locale locale) {
        assert locale != null : "Null locale is not supported!";

        checkHasLock();
        this.locale = locale;

        getUIs().forEach(ui -> ui.setLocale(locale));
    }

    /**
     * Gets the session's error handler.
     *
     * @return the current error handler
     */
    public ErrorHandler getErrorHandler() {
        checkHasLock();
        return errorHandler;
    }

    /**
     * Sets the session error handler.
     *
     * @param errorHandler
     *            the new error handler
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        checkHasLock();
        this.errorHandler = errorHandler;
    }

    /**
     * Adds a request handler to this session. Request handlers can be added to
     * provide responses to requests that are not handled by the default
     * functionality of the framework.
     * <p>
     * Handlers are called in reverse order of addition, so the most recently
     * added handler will be called first.
     *
     * @param handler
     *            the request handler to add
     * @see #removeRequestHandler(RequestHandler)
     */
    public void addRequestHandler(RequestHandler handler) {
        checkHasLock();
        requestHandlers.addFirst(handler);
    }

    /**
     * Removes a request handler from the session.
     *
     * @param handler
     *            the request handler to remove
     */
    public void removeRequestHandler(RequestHandler handler) {
        checkHasLock();
        requestHandlers.remove(handler);
    }

    /**
     * Gets the request handlers that are registered to the session. The
     * iteration order of the returned collection is the same as the order in
     * which the request handlers will be invoked when a request is handled.
     *
     * @return a collection of request handlers, with the iteration order
     *         according to the order they would be invoked
     * @see #addRequestHandler(RequestHandler)
     * @see #removeRequestHandler(RequestHandler)
     */
    public Collection<RequestHandler> getRequestHandlers() {
        checkHasLock();
        return Collections.unmodifiableCollection(requestHandlers);
    }

    /**
     * Gets the currently used session. The current session is automatically
     * defined when processing requests to the server (see {@link ThreadLocal})
     * and in {@link VaadinSession#access(Command)} and
     * {@link UI#access(Command)}. In other cases, (e.g. from background
     * threads), the current session is not automatically defined.
     * <p>
     * The session is stored using a weak reference to avoid leaking memory in
     * case it is not explicitly cleared.
     *
     * @return the current session instance if available, otherwise
     *         <code>null</code>
     * @see #setCurrent(VaadinSession)
     */
    public static VaadinSession getCurrent() {
        return CurrentInstance.get(VaadinSession.class);
    }

    /**
     * Sets the thread local for the current session. This method is used by the
     * framework to set the current session whenever a new request is processed
     * and it is cleared when the request has been processed.
     * <p>
     * The application developer can also use this method to define the current
     * session outside the normal request handling and treads started from
     * request handling threads, e.g. when initiating custom background threads.
     * <p>
     * The session is stored using a weak reference to avoid leaking memory in
     * case it is not explicitly cleared.
     *
     * @param session
     *            the session to set as current
     * @see #getCurrent()
     * @see ThreadLocal
     */
    public static void setCurrent(VaadinSession session) {
        CurrentInstance.set(VaadinSession.class, session);
    }

    /**
     * Gets all the UIs of this session. This includes UIs that have been
     * requested but not yet initialized. UIs that receive no heartbeat requests
     * from the client are eventually removed from the session.
     *
     * @return a collection of UIs belonging to this application
     */
    public Collection<UI> getUIs() {
        checkHasLock();
        return Collections.unmodifiableCollection(uIs.values());
    }

    /**
     * Returns a UI with the given id.
     * <p>
     * This is meant for framework internal use.
     *
     * @param uiId
     *            The UI id
     * @return The UI with the given id or null if not found
     */
    public UI getUIById(int uiId) {
        checkHasLock();
        return uIs.get(uiId);
    }

    /**
     * Checks if the current thread has exclusive access to this
     * <code>VaadinSession</code>.
     *
     * @return true if the thread has exclusive access, false otherwise
     */
    public boolean hasLock() {
        ReentrantLock l = ((ReentrantLock) getLockInstance());
        return l.isHeldByCurrentThread();
    }

    /**
     * Potentially checks whether this session is currently locked by the
     * current thread, and fails with the given message if not.
     * <p>
     * When production mode is enabled, the check is only done if assertions are
     * also enabled. This is done to avoid the small performance impact of
     * continuously checking the lock status. The check is always done when
     * production mode is not enabled.
     *
     * @param message
     *            the error message to include when failing if the check is done
     *            and the session is not locked
     */
    public void checkHasLock(String message) {
        if (configuration == null || configuration.isProductionMode()) {
            assert hasLock() : message;
        } else if (!hasLock()) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Potentially checks whether this session is currently locked by the
     * current thread, and fails with a standard error message if not.
     * <p>
     * When production mode is enabled, the check is only done if assertions are
     * also enabled. This is done to avoid the small performance impact of
     * continuously checking the lock status. The check is always done when
     * production mode is not enabled.
     */
    public void checkHasLock() {
        checkHasLock(SESSION_NOT_LOCKED_MESSAGE);
    }

    /**
     * Checks if the current thread has exclusive access to the given
     * WrappedSession.
     *
     * @param service
     *            the service to check
     * @param session
     *            the session to use for checking
     * @return true if this thread has exclusive access, false otherwise
     */
    protected static boolean hasLock(VaadinService service,
            WrappedSession session) {
        ReentrantLock l = (ReentrantLock) service.getSessionLock(session);
        return l.isHeldByCurrentThread();
    }

    /**
     * Called by the framework to remove an UI instance from the session because
     * it has been closed.
     *
     * @param ui
     *            the UI to remove
     */
    public void removeUI(UI ui) {
        checkHasLock();
        assert UI.getCurrent() != null : "Current UI cannot be null";
        assert ui != null : "Removed UI cannot be null";
        assert UI.getCurrent().getUIId() == ui.getUIId() : "UIs don't match";
        ui.getInternals().setSession(null);
        uIs.remove(ui.getUIId());
    }

    /**
     * Gets the {@link Lock} instance that is used for protecting the data of
     * this session from concurrent access.
     * <p>
     * The <code>Lock</code> can be used to gain more control than what is
     * available only using {@link #lock()} and {@link #unlock()}. The returned
     * instance is not guaranteed to support any other features of the
     * <code>Lock</code> interface than {@link Lock#lock()} and
     * {@link Lock#unlock()}.
     *
     * @return the <code>Lock</code> that is used for synchronization, never
     *         <code>null</code>
     * @see #lock()
     * @see Lock
     */
    public Lock getLockInstance() {
        return lock;
    }

    /**
     * Locks this session to protect its data from concurrent access. Accessing
     * the UI state from outside the normal request handling should always lock
     * the session and unlock it when done. The preferred way to ensure locking
     * is done correctly is to wrap your code using {@link UI#access(Command)}
     * (or {@link VaadinSession#access(Command)} if you are only touching the
     * session and not any UI), e.g.:
     *
     * <pre>
     * myUI.access(new Command() {
     *     &#064;Override
     *     public void run() {
     *         // Here it is safe to update the UI.
     *         // UI.getCurrent can also be used
     *         myUI.getContent().setCaption(&quot;Changed safely&quot;);
     *     }
     * });
     * </pre>
     *
     * If you for whatever reason want to do locking manually, you should do it
     * like:
     *
     * <pre>
     * session.lock();
     * try {
     *     doSomething();
     * } finally {
     *     session.unlock();
     * }
     * </pre>
     *
     * This method will block until the lock can be retrieved.
     * <p>
     * {@link #getLockInstance()} can be used if more control over the locking
     * is required.
     *
     * @see #unlock()
     * @see #getLockInstance()
     * @see #hasLock()
     */
    public void lock() {
        getLockInstance().lock();
    }

    /**
     * Unlocks this session. This method should always be used in a finally
     * block after {@link #lock()} to ensure that the lock is always released.
     * <p>
     * For UIs in this session that have its push mode set to
     * {@link PushMode#AUTOMATIC automatic}, pending changes will be pushed to
     * their respective clients.
     *
     * @see #lock()
     * @see UI#push()
     */
    public void unlock() {
        checkHasLock();
        boolean ultimateRelease = false;
        try {
            /*
             * Run pending tasks and push if the reentrant lock will actually be
             * released by this unlock() invocation.
             */
            if (((ReentrantLock) getLockInstance()).getHoldCount() == 1) {
                ultimateRelease = true;
                getService().runPendingAccessTasks(this);

                for (UI ui : getUIs()) {
                    if (ui.getPushConfiguration()
                            .getPushMode() == PushMode.AUTOMATIC) {
                        Map<Class<?>, CurrentInstance> oldCurrent = CurrentInstance
                                .setCurrent(ui);
                        try {
                            ui.push();
                        } finally {
                            CurrentInstance.restoreInstances(oldCurrent);
                        }
                    }
                }
            }
        } finally {
            getLockInstance().unlock();
        }

        /*
         * If the session is locked when a new access task is added, it is
         * assumed that the queue will be purged when the lock is released. This
         * might however not happen if a task is enqueued between the moment
         * when unlock() purges the queue and the moment when the lock is
         * actually released. This means that the queue should be purged again
         * if it is not empty after unlocking.
         */
        if (ultimateRelease && !getPendingAccessQueue().isEmpty()) {
            getService().ensureAccessQueuePurged(this);
        }
    }

    /**
     * Stores a value in this service session. This can be used to associate
     * data with the current user so that it can be retrieved at a later point
     * from some other part of the application. Setting the value to
     * <code>null</code> clears the stored value.
     *
     * @param name
     *            the name to associate the value with, can not be
     *            <code>null</code>
     * @param value
     *            the value to associate with the name, or <code>null</code> to
     *            remove a previous association.
     * @see #getAttribute(String)
     */
    public void setAttribute(String name, Object value) {
        checkHasLock();
        attributes.setAttribute(name, value);
    }

    /**
     * Stores a value in this service session. This can be used to associate
     * data with the current user so that it can be retrieved at a later point
     * from some other part of the application. Setting the value to
     * <code>null</code> clears the stored value.
     * <p>
     * The fully qualified name of the type is used as the name when storing the
     * value. The outcome of calling this method is thus the same as if calling
     * <p>
     * <code>setAttribute(type.getName(), value);</code>
     *
     * @param type
     *            the type that the stored value represents, can not be null
     * @param value
     *            the value to associate with the type, or <code>null</code> to
     *            remove a previous association.
     * @param <T>
     *            the type of the stored value
     * @see #getAttribute(Class)
     * @see #setAttribute(String, Object)
     */
    public <T> void setAttribute(Class<T> type, T value) {
        checkHasLock();
        attributes.setAttribute(type, value);
    }

    /**
     * Gets a stored attribute value. If a value has been stored for the
     * session, that value is returned. If no value is stored for the name,
     * <code>null</code> is returned.
     *
     * @param name
     *            the name of the value to get, can not be <code>null</code>.
     * @return the value, or <code>null</code> if no value has been stored or if
     *         it has been set to null.
     * @see #setAttribute(String, Object)
     */
    public Object getAttribute(String name) {
        checkHasLock();
        return attributes.getAttribute(name);
    }

    /**
     * Gets a stored attribute value. If a value has been stored for the
     * session, that value is returned. If no value is stored for the name,
     * <code>null</code> is returned.
     * <p>
     * The fully qualified name of the type is used as the name when getting the
     * value. The outcome of calling this method is thus the same as if calling
     * <br>
     * <br>
     * <code>getAttribute(type.getName());</code>
     *
     * @param type
     *            the type of the value to get, can not be <code>null</code>.
     * @param <T>
     *            the type of the value to get
     * @return the value, or <code>null</code> if no value has been stored or if
     *         it has been set to null.
     * @see #setAttribute(Class, Object)
     * @see #getAttribute(String)
     */
    public <T> T getAttribute(Class<T> type) {
        checkHasLock();
        return attributes.getAttribute(type);
    }

    /**
     * Creates a new unique id for a UI.
     *
     * @return a unique UI id
     */
    public int getNextUIid() {
        checkHasLock();
        return nextUIId++;
    }

    /**
     * Adds an initialized UI to this session.
     *
     * @param ui
     *            the initialized UI to add.
     */
    public void addUI(UI ui) {
        checkHasLock();
        if (ui.getUIId() == -1) {
            throw new IllegalArgumentException(
                    "Can not add an UI that has not been initialized.");
        }
        if (ui.getSession() != this) {
            throw new IllegalArgumentException(
                    "The UI belongs to a different session");
        }

        uIs.put(ui.getUIId(), ui);
    }

    public VaadinService getService() {
        return service;
    }

    /**
     * Sets this session to be closed and all UI state to be discarded at the
     * end of the current request, or at the end of the next request if there is
     * no ongoing one.
     * <p>
     * After the session has been discarded, any UIs that have been left open
     * will give a Session Expired error and a new session will be created for
     * serving new UIs.
     *
     * @see SystemMessages#getSessionExpiredCaption()
     */
    public void close() {
        checkHasLock();
        state = VaadinSessionState.CLOSING;
    }

    /**
     * Returns the lifecycle state of this session.
     *
     * @return the current state
     */
    public VaadinSessionState getState() {
        checkHasLock();
        return state;
    }

    /**
     * Sets the lifecycle state of this session. The allowed transitions are
     * OPEN to CLOSING and CLOSING to CLOSED.
     *
     * @param state
     *            the new state
     */
    protected void setState(VaadinSessionState state) {
        checkHasLock();
        assert isValidChange(state) : "Invalid session state change "
                + this.state + "->" + state;

        this.state = state;
    }

    private boolean isValidChange(VaadinSessionState newState) {
        return (state == VaadinSessionState.OPEN
                && newState == VaadinSessionState.CLOSING)
                || (state == VaadinSessionState.CLOSING
                        && newState == VaadinSessionState.CLOSED);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinSession.class.getName());
    }

    /**
     * Locks this session and runs the provided Command right away.
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
     * <li>If the current thread is currently holding the lock of this session,
     * {@link #accessSynchronously(Command)} runs the task right away whereas
     * {@link #access(Command)} defers the task to a later point in time.</li>
     * <li>If some other thread is currently holding the lock for this session,
     * {@link #accessSynchronously(Command)} blocks while waiting for the lock
     * to be available whereas {@link #access(Command)} defers the task to a
     * later point in time.</li>
     * </ul>
     *
     * @param command
     *            the command which accesses the session
     * @throws IllegalStateException
     *             if the current thread holds the lock for another session
     * @see #lock()
     * @see #getCurrent()
     * @see #access(Command)
     * @see UI#accessSynchronously(Command)
     */
    public void accessSynchronously(Command command) {
        VaadinService.verifyNoOtherSessionLocked(this);

        Map<Class<?>, CurrentInstance> old = null;
        lock();
        try {
            old = CurrentInstance.setCurrent(this);
            command.execute();
        } finally {
            unlock();
            if (old != null) {
                CurrentInstance.restoreInstances(old);
            }
        }

    }

    /**
     * Provides exclusive access to this session from outside a request handling
     * thread.
     * <p>
     * The given command is executed while holding the session lock to ensure
     * exclusive access to this session. If this session is not locked, the lock
     * will be acquired and the command is run right away. If this session is
     * currently locked, the command will be run before that lock is released.
     * <p>
     * RPC handlers for components inside this session do not need to use this
     * method as the session is automatically locked by the framework during RPC
     * handling.
     * <p>
     * Please note that the command might be invoked on a different thread or
     * later on the current thread, which means that custom thread locals might
     * not have the expected values when the command is executed.
     * {@link VaadinSession#getCurrent()} and {@link VaadinService#getCurrent()}
     * are set according to this session before executing the command. Other
     * standard CurrentInstance values such as
     * {@link VaadinService#getCurrentRequest()} and
     * {@link VaadinService#getCurrentResponse()} will not be defined.
     * <p>
     * The returned future can be used to check for task completion and to
     * cancel the task. To help avoiding deadlocks, {@link Future#get()} throws
     * an exception if it is detected that the current thread holds the lock for
     * some other session.
     *
     * @param command
     *            the command which accesses the session
     * @return a future that can be used to check for task completion and to
     *         cancel the task
     * @see #lock()
     * @see #getCurrent()
     * @see #accessSynchronously(Command)
     * @see UI#access(Command)
     */
    public Future<Void> access(Command command) {
        return getService().accessSession(this, command);
    }

    /**
     * Gets the queue of tasks submitted using {@link #access(Command)}. It is
     * safe to call this method and access the returned queue without holding
     * the {@link #lock() session lock}.
     *
     * @return the queue of pending access tasks
     */
    public Queue<FutureAccess> getPendingAccessQueue() {
        return pendingAccessQueue;
    }

    /**
     * Gets the push connection identifier for this session. Used when
     * establishing a push connection with the client.
     *
     * @return the push connection identifier string
     */
    public String getPushId() {
        checkHasLock();
        return pushId;
    }

    /**
     * Override default deserialization logic to account for transient
     * {@link #pendingAccessQueue}.
     *
     * @param stream
     *            the object to read
     * @throws IOException
     *             if an IO error occurred
     * @throws ClassNotFoundException
     *             if the class of the stream object could not be found
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        Map<Class<?>, CurrentInstance> old = CurrentInstance.setCurrent(this);
        try {
            stream.defaultReadObject();
            pendingAccessQueue = new ConcurrentLinkedQueue<>();
        } finally {
            CurrentInstance.restoreInstances(old);
        }
    }

    /**
     * Refreshes the transient fields of the session to ensure they are up to
     * date.
     * <p>
     * Called internally by the framework.
     *
     * @param wrappedSession
     *            the session this VaadinSession is stored in
     * @param vaadinService
     *            the service associated with this VaadinSession
     */
    public void refreshTransients(WrappedSession wrappedSession,
            VaadinService vaadinService) {
        session = wrappedSession;
        service = vaadinService;
        refreshLock();
    }

    /**
     * Get resource registry instance.
     * <p>
     * Use this instance to manage {@link StreamResource}s.
     *
     * @return resource registry
     */
    public StreamResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

}
