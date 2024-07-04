package com.vaadin.flow.server.dau;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.SystemMessagesInfo;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.pro.licensechecker.dau.DauIntegration;
import com.vaadin.pro.licensechecker.dau.EnforcementException;

/**
 * A utility class for various daily active users collecting methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
 */
public final class DAUUtils {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DAUUtils.class);

    static final String DAU_COOKIE_NAME = "DailyActiveUser";

    // After 24H a user is treated as a new user for next day
    static final int DAU_COOKIE_MAX_AGE_IN_SECONDS = 24 * 3600;

    static final long DAU_MIN_ACTIVITY_IN_SECONDS = 60L;
    public static final String ENFORCEMENT_EXCEPTION_KEY = DAUUtils.class
            .getName() + ".EnforcementException";

    private DAUUtils() {
    }

    /**
     * Looks up for a daily active user tracking cookie in a given http request
     * from browser.
     *
     * @param request
     *            Vaadin request
     * @return optional cookie object, may be missing, if users tracking is
     *         disabled or if this request is an initial request from client.
     */
    public static Optional<Cookie> getTrackingCookie(VaadinRequest request) {
        Cookie[] cookies = Objects
                .requireNonNull(request, "Request must not be null")
                .getCookies();
        if (cookies == null) {
            return Optional.empty();
        } else {
            return Arrays.stream(cookies)
                    .filter(cookie -> DAU_COOKIE_NAME.equals(cookie.getName()))
                    .findAny();
        }
    }

    /**
     * Generates a new cookie for counting daily active users within 24 hour
     * time interval.
     * <p>
     * </p>
     * Cookie value is formatted as {@code  trackingHash$creationTime}, with
     * {@code creationTime} expressed as number of milliseconds from the epoch
     * of 1970-01-01T00:00:00Z. The cookie creation time is required on
     * subsequent requests to detect active users. By default, the cookie
     * expires after 24 hours.
     *
     * @param request
     *            http request from browser
     * @return http cookie to be used to count application's end-users daily
     */
    public static Cookie generateNewCookie(VaadinRequest request) {
        String cookieValue = DauIntegration.newTrackingHash() + '$'
                + Instant.now().toEpochMilli();
        Cookie cookie = new Cookie(DAU_COOKIE_NAME, cookieValue);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(DAU_COOKIE_MAX_AGE_IN_SECONDS);
        cookie.setPath("/");
        return cookie;
    }

    /**
     * Parses DAU cookie value to extract tracking information.
     * <p>
     * </p>
     * Cookie value is expected to be in format
     * {@literal trackingHash$creationTime}, with {@literal creationTime}
     * expressed as the number of milliseconds from the epoch of
     * 1970-01-01T00:00:00Z since the cookie creation instant. An empty Optional
     * is returned if the cookie value is not in the expected format or if the
     * creation time is invalid.
     *
     * @param cookie
     *            The DAU cookie.
     * @return a data structure representing the information stored in the
     *         cookie, or an empty Optional if the cookie value is malformed or
     *         contains invalid data.
     * @see #generateNewCookie(VaadinRequest)
     */
    static Optional<DauCookie> parserCookie(Cookie cookie) {
        String cookieValue = cookie.getValue();
        String[] tokens = cookieValue.split("\\$");
        if (tokens.length != 2) {
            LOGGER.debug("Invalid DAU cookie value: {}.", cookieValue);
            return Optional.empty();
        }

        String trackingHash = tokens[0];
        if (trackingHash.isBlank()) {
            LOGGER.debug("Invalid DAU cookie value: {}. Missing tracking hash",
                    cookieValue);
            return Optional.empty();
        }
        Instant creationTime;
        try {
            creationTime = Instant.ofEpochMilli(Long.parseLong(tokens[1]));
        } catch (NumberFormatException e) {
            LOGGER.debug(
                    "Invalid DAU cookie value: {}. Unparsable creation timestamp.",
                    cookieValue);
            return Optional.empty();
        }
        return Optional.of(new DauCookie(trackingHash, creationTime));
    }

    /**
     * Creates a JSON message which, when sent to client as-is, will cause a
     * critical error to be shown with the given details.
     *
     * @param vaadinRequest
     *            the current Vaadin request.
     * @param enforcementException
     *            the enforcement exception raised by license checker.
     * @return an error messages in JSON format to be sent to the client.
     */
    public static String jsonEnforcementResponse(VaadinRequest vaadinRequest,
            DauEnforcementException enforcementException) {
        EnforcementNotificationMessages messages = getEnforcementNotificationMessages(
                vaadinRequest);
        return VaadinService.createCriticalNotificationJSON(messages.caption(),
                messages.message(), messages.details(), messages.url());
    }

    /**
     * Gets the enforcement messages for the given request.
     * <p>
     * </p>
     * Enforcement messages are get from the registered {@link DAUCustomizer},
     * if available. Otherwise, the default messages are returned.
     *
     * @param vaadinRequest
     *            The current request
     * @return enforcement messages for the request.
     * @see DAUCustomizer#getEnforcementNotificationMessages(SystemMessagesInfo)
     * @see EnforcementNotificationMessages#DEFAULT
     */
    public static EnforcementNotificationMessages getEnforcementNotificationMessages(
            VaadinRequest vaadinRequest) {
        EnforcementNotificationMessages messages = EnforcementNotificationMessages.DEFAULT;
        VaadinService service = vaadinRequest.getService();
        if (service != null) {
            DAUCustomizer dauCustomizer = service.getContext()
                    .getAttribute(DAUCustomizer.class);
            if (dauCustomizer != null) {
                SystemMessagesInfo systemMessagesInfo = new SystemMessagesInfo(
                        HandlerHelper.findLocale(VaadinSession.getCurrent(),
                                vaadinRequest),
                        vaadinRequest, service);
                messages = dauCustomizer
                        .getEnforcementNotificationMessages(systemMessagesInfo);
            }
        }
        return messages;
    }

    /**
     * A helper to mark operations that might be eligible for DAU tracking
     */
    public enum TrackableOperation {
        INSTANCE;

        /**
         * Executes the given operation, marking it as eligible for DAU
         * tracking.
         *
         * @param operation
         *            the operation to execute.
         */
        public void execute(Runnable operation) {
            CurrentInstance.set(TrackableOperation.class,
                    TrackableOperation.INSTANCE);
            try {
                operation.run();
            } finally {
                CurrentInstance.set(TrackableOperation.class, null);
            }
        }

        /**
         * Gets if the current request has been marked for DAU tracking.
         *
         * @return {@literal true} if DAU tracking should be applied to the
         *         current request, otherwise {@literal false}.
         */
        public boolean isTrackable() {
            return CurrentInstance.get(TrackableOperation.class) != null;
        }
    }

    record DauCookie(String trackingHash, Instant creationTime) {
        public boolean isActive() {
            return creationTime
                    .plus(DAU_MIN_ACTIVITY_IN_SECONDS, ChronoUnit.SECONDS)
                    .isBefore(Instant.now());
        }
    }

    private record TrackingDetails(String trackingHash, String userIdentity) {
    }

    /**
     * Gets if a request should be considered for DAU tracking or not.
     * <p>
     * </p>
     * Request that should be taken into account for DAU tracking are:
     *
     * <ul>
     * <li>INIT request.</li>
     * <li>UIDL requests</li>
     * <li>PUSH requests, with WEBSOCKET transport. For other transports
     * interaction happens as UIDL request on an HTTP request</li>
     * </ul>
     *
     * @param request
     *            the Vaadin request.
     * @return {@literal true} if DAU user tracking should be applied for the
     *         given request, otherwise {@literal false}.
     */
    static boolean isTrackableRequest(VaadinRequest request) {
        return HandlerHelper.isRequestType(request,
                HandlerHelper.RequestType.INIT)
                || HandlerHelper.isRequestType(request,
                        HandlerHelper.RequestType.UIDL)
                // PUSH request with WEBSOCKET transport, ignoring connect
                // or Hilla endpoint invocation
                || TrackableOperation.INSTANCE.isTrackable()
                || isDirectViewRequest(request);
    }

    private static boolean isKnownPublicResource(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        return Stream
                .concat(Stream.of(HandlerHelper.getPublicResources()),
                        Stream.of(HandlerHelper.getPublicResourcesRoot()))
                .anyMatch(path -> path.equals(pathInfo)) || isPwaIcon(request);
    }

    private static boolean isPwaIcon(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        if (request.getService()
                .getContext() instanceof VaadinServletContext vaadinContext) {
            PwaConfiguration pwaConfiguration = PwaRegistry
                    .getInstance(vaadinContext.getContext())
                    .getPwaConfiguration();
            if (pwaConfiguration.isEnabled()) {
                return HandlerHelper
                        .getIconVariants(pwaConfiguration.getIconPath())
                        .contains(pathInfo);
            }
        }
        return false;
    }

    private static boolean isDirectViewRequest(VaadinRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.isEmpty()
                    || "/".equals(pathInfo)) {
                return true;
            }
            if (pathInfo.startsWith("/VAADIN/")
                    || pathInfo.startsWith("/HILLA/")) {
                return false;
            }
            return !isKnownPublicResource(request);
        }
        return false;
    }

    /**
     * Tracks the current user with the given optional identity.
     * <p>
     * </p>
     * Tracking the user may raise an enforcement exception, that is stored and
     * applied later by calling
     * {@link #applyEnforcement(VaadinRequest, Predicate)} method.
     * <p>
     * </p>
     * Tracking for UIDL requests is postponed until the message is parsed, to
     * prevent UI poll events to be considered as user interaction.
     *
     * @param request
     *            the Vaadin request.
     * @param trackingHash
     *            user tracking hash, never {@literal null}.
     * @param userIdentity
     *            user identity, can be {@literal null}.
     */
    static void trackUser(VaadinRequest request, String trackingHash,
            String userIdentity) {
        if (HandlerHelper.isRequestType(request,
                HandlerHelper.RequestType.UIDL)) {
            // postpone tracking for UIDL requests to ServerRpcHandler to
            // prevent counting and blocking poll requests, that are not
            // consider active interaction with the application
            request.setAttribute(TrackingDetails.class.getName(),
                    new TrackingDetails(trackingHash, userIdentity));
        } else {
            try {
                DauIntegration.trackUser(trackingHash, userIdentity);
            } catch (EnforcementException ex) {
                // request will be blocked in ServerRpcHandler to prevent
                // blocking poll requests, that are not consider active
                // interaction with the application
                request.setAttribute(ENFORCEMENT_EXCEPTION_KEY, ex);
            }
        }
    }

    /**
     * Potentially applies enforcement to the current request if DAU limit is
     * exceeded.
     * <p>
     * </p>
     * If enforcement has to be applied an {@link EnforcementException} is
     * thrown.
     *
     * @param request
     *            the Vaadin request
     * @param enforceableRequest
     *            predicate to check if the request can be blocked or not.
     * @throws DauEnforcementException
     *             if request must be blocked because of DAU limit exceeded.
     */
    public static void applyEnforcement(VaadinRequest request,
            Predicate<VaadinRequest> enforceableRequest) {
        TrackingDetails trackingDetails = (TrackingDetails) request
                .getAttribute(TrackingDetails.class.getName());
        EnforcementException enforcementException = (EnforcementException) request
                .getAttribute(ENFORCEMENT_EXCEPTION_KEY);
        try {
            if ((enforcementException != null || trackingDetails != null)
                    && enforceableRequest.test(request)) {
                if (trackingDetails != null) {
                    try {
                        DauIntegration.trackUser(trackingDetails.trackingHash(),
                                trackingDetails.userIdentity());
                    } catch (EnforcementException ex) {
                        enforcementException = ex;
                    }
                }
                if (enforcementException != null) {
                    throw new DauEnforcementException(enforcementException);
                }
            }
        } finally {
            request.removeAttribute(ENFORCEMENT_EXCEPTION_KEY);
            request.removeAttribute(TrackingDetails.class.getName());
        }
    }
}
