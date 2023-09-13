package com.vaadin.flow.server;

import io.micrometer.common.lang.Nullable;
import io.micrometer.jakarta.instrument.binder.http.DefaultHttpJakartaServerServletRequestObservationConvention;
import io.micrometer.jakarta.instrument.binder.http.HttpJakartaServerServletRequestObservationContext;
import io.micrometer.jakarta.instrument.binder.http.HttpJakartaServerServletRequestObservationConvention;
import io.micrometer.jakarta.instrument.binder.http.JakartaHttpObservationDocumentation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Micrometer Observation {@link VaadinFilter} that will start
 * observations around processing of a request.
 *
 * @author Marcin Grzejszczak
 * @since 24.2
 */
public class ObservedVaadinFilter implements VaadinFilter {

    private static final String SCOPE_ATTRIBUTE = ObservedVaadinFilter.class.getName() + ".scope";

    private final ObservationRegistry observationRegistry;

    private final HttpJakartaServerServletRequestObservationConvention convention;

    public ObservedVaadinFilter(ObservationRegistry observationRegistry,
                                @Nullable HttpJakartaServerServletRequestObservationConvention convention) {
        this.observationRegistry = observationRegistry;
        this.convention = convention;
    }

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        if (request instanceof VaadinServletRequest servletRequest && response instanceof VaadinServletResponse servletResponse) {
            HttpJakartaServerServletRequestObservationContext context = new HttpJakartaServerServletRequestObservationContext(servletRequest, servletResponse);
            Observation observation = JakartaHttpObservationDocumentation.JAKARTA_SERVLET_SERVER_OBSERVATION.start(this.convention, DefaultHttpJakartaServerServletRequestObservationConvention.INSTANCE, () -> context, observationRegistry);
            request.setAttribute(SCOPE_ATTRIBUTE, observation.openScope());
        }
    }

    @Override
    public void handleException(VaadinRequest request, VaadinResponse response, VaadinSession vaadinSession, Exception t) {
        Observation.Scope scope = (Observation.Scope) request.getAttribute(SCOPE_ATTRIBUTE);
        if (scope == null) {
            return;
        }
        scope.getCurrentObservation().error(t);
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
        Observation.Scope scope = (Observation.Scope) request.getAttribute(SCOPE_ATTRIBUTE);
        if (scope == null) {
            return;
        }
        scope.close();
        Observation observation = scope.getCurrentObservation();
        if (!observation.isNoop() && response instanceof HttpServletResponse httpServletResponse) {
            HttpJakartaServerServletRequestObservationContext ctx = (HttpJakartaServerServletRequestObservationContext) observation.getContext();
            ctx.setResponse(httpServletResponse);
        }
        observation.stop();
    }
}
