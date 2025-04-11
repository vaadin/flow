package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;

/**
 * Request handler callback for handing client-server or server-client data
 * transfer for a specific element.
 */
@FunctionalInterface
public interface ElementRequestHandler extends Serializable {

    /**
     * Request handler callback for handing client-server or server-client data
     * transfer for a specific element.
     *
     * @param request
     *            VaadinRequest request to handle
     * @param response
     *            VaadinResponse response to handle
     * @param session
     *            VaadinSession current session
     * @param owner
     *            Element owner element
     */
    void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner);

    /**
     * Optional URL postfix allows appending an application-controlled string,
     * e.g. the logical name of the target file, to the end of the otherwise
     * random-looking download URL. If defined, requests that would otherwise be
     * routable are still rejected if the postfix is missing or invalid. Postfix
     * changes the last segment in the resource url.
     *
     * @return String optional URL postfix, or {@code null} for "".
     */
    default String getUrlPostfix() {
        return null;
    }

    /**
     * Whether to invoke this request handler even if the owning element is
     * currently inert.
     *
     * @return {@code true} to invoke for inert elements, {@code false}
     *         otherwise. Defaults to {@code false}.
     */
    default boolean allowInert() {
        return false;
    }

    /**
     * Controls whether request handler is invoked when the owner element is
     * disabled.
     *
     * @return the most permissive DisabledUpdateMode for this request handler.
     */
    default DisabledUpdateMode getDisabledUpdateMode() {
        return DisabledUpdateMode.ONLY_WHEN_ENABLED;
    }
}
