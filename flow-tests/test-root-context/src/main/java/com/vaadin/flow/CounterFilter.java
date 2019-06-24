package com.vaadin.flow;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import java.io.IOException;

import com.vaadin.flow.uitest.ui.TestingServiceInitListener;

@WebFilter(urlPatterns = "/view/*", asyncSupported = true)
public class CounterFilter implements javax.servlet.Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (request.getParameter("resetUisCounter") != null) {
            TestingServiceInitListener.resetUisCount();
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

}