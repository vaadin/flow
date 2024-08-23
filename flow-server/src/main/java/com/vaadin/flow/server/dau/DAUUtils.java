package com.vaadin.flow.server.dau;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.SystemMessagesInfo;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

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
     * Checks if Daily Active User integration is enabled for the application.
     *
     * @param service
     *            the VaadinService instance.
     * @return {@literal true} if DAU integration is enabled, otherwise
     *         {@literal false}.
     */
    public static boolean isDauEnabled(VaadinService service) {
        // TODO: force removal of dau.enable system property to check only on
        // flow-build-info?
        System.clearProperty("vaadin." + Constants.DAU_TOKEN);
        return service.getDeploymentConfiguration().isProductionMode()
                && service.getDeploymentConfiguration()
                        .getBooleanProperty(Constants.DAU_TOKEN, false);
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
     * @see FlowDauIntegration#generateNewCookie(VaadinRequest)
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

    private static VaadinRequest createVaadinRequest(
            VaadinService defaultVaadinService, HttpServletRequest request) {
        VaadinService service = VaadinService.getCurrent();
        if (service == null) {
            service = defaultVaadinService;
        }
        if (!(service instanceof VaadinServletService)) {
            // Should never happen, but will prevent a class cast exception on
            // request creation
            service = null;
        }
        return new VaadinServletRequest(request,
                (VaadinServletService) service);
    }

    /**
     * Record to hold the DAU enforcement check result with the messages and the
     * action to be run at the end of the request.
     *
     * @param messages
     *            the enforcement messages or null if enforcement should not be
     *            applied
     * @param origin
     *          the exception that caused the enforcement
     * @param endRequestAction
     *            the action to be run at the end of the request
     */
    public record EnforcementResult(EnforcementNotificationMessages messages,
            RuntimeException origin, Runnable endRequestAction) {

        public boolean isEnforcementNeeded() {
            return messages != null;
        }
    }

    /**
     * Track DAU and check if enforcement should apply to the given request. If
     * enforcement is needed, the enforcement messages are returned.
     * <p>
     * </p>
     * Method checks if the current request should be considered for DAU
     * tracking by using {@link #isDauEnabled(VaadinService)}.
     *
     * @param defaultVaadinService
     *            the default VaadinService to use if the current service is not
     *            available
     * @param request
     *            the request. Cannot be null.
     * @param response
     *            the response
     * @return the enforcement result. Never null.
     */
    public static EnforcementResult trackDAU(VaadinService defaultVaadinService,
            HttpServletRequest request, HttpServletResponse response) {
        assert request != null;

        VaadinRequest vaadinRequest = createVaadinRequest(defaultVaadinService,
                request);
        VaadinService service = vaadinRequest.getService();
        VaadinResponse vaadinResponse = (response != null)
                ? new VaadinServletResponse(response,
                        (VaadinServletService) service)
                : null;

        Runnable endRequestAction = null;
        if (service != null) {
            endRequestAction = () -> {
                // Do not provide VaadinResponse to prevent interceptor to alter
                // the http response
                service.requestEnd(vaadinRequest, null, null);
            };
            try {
                DAUUtils.TrackableOperation.INSTANCE.execute(() -> {
                    service.requestStart(vaadinRequest, vaadinResponse);
                    if (DAUUtils.isDauEnabled(service)) {
                        FlowDauIntegration.applyEnforcement(vaadinRequest,
                                unused -> true);
                    }
                });
            } catch (DauEnforcementException e) {
                EnforcementNotificationMessages messages = DAUUtils
                        .getEnforcementNotificationMessages(vaadinRequest);
                return new EnforcementResult(messages, e, endRequestAction);
            }
        }
        return new EnforcementResult(null, null, endRequestAction);
    }
}
