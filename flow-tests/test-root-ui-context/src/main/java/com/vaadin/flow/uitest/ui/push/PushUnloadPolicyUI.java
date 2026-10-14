package com.vaadin.flow.uitest.ui.push;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;

/*
 * Note that @Push is generally not supported in this location, but instead
 * explicitly picked up by logic in the BasicPushUI constructor.
 */
@Push(transport = Transport.LONG_POLLING)
public class PushUnloadPolicyUI extends BasicPushLongPollingUI {

    /**
     * Blocks unload listeners on the page of this UI only, so that the rest of
     * the test UIs keep the browser default. The policy is set explicitly
     * because a browser driven by ChromeDriver does not block unload by
     * default.
     */
    @WebFilter("/run/com.vaadin.flow.uitest.ui.push.PushUnloadPolicyUI")
    public static class UnloadPolicyFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                FilterChain chain) throws IOException, ServletException {
            ((HttpServletResponse) response).setHeader("Permissions-Policy",
                    "unload=()");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    }
}
