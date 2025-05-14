package com.vaadin.flow.spring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.security.web.DefaultRedirectStrategy;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HandlerHelper;

/**
 * A strategy to handle redirects which is aware of UIDL requests.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlRedirectStrategy extends DefaultRedirectStrategy {

    @Override
    public void sendRedirect(HttpServletRequest request,
            HttpServletResponse response, String url) throws IOException {
        final var servletMapping = request.getHttpServletMapping().getPattern();
        if (HandlerHelper.isFrameworkInternalRequest(servletMapping, request)) {
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.getPage().setLocation(url);
            } else {
                LoggerFactory.getLogger(UidlRedirectStrategy.class).warn(
                        "A redirect to {} was request during a Vaadin request, "
                                + "but it was not possible to get the UI instance to perform the action.",
                        url);
            }
        } else if (response == null) {
            LoggerFactory.getLogger(UidlRedirectStrategy.class)
                    .warn("A redirect to {} was request, "
                            + "but it has null HttpServletResponse and can't perform the action. "
                            + "Performing logout during a Vaadin request with @Push(transport = Transport.WEBSOCKET) would cause this.",
                            url);
        } else {
            super.sendRedirect(request, response, url);
        }
    }
}
