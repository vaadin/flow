/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

@WebServlet(urlPatterns = "/*", asyncSupported = true)
public class CustomServlet extends VaadinServlet {

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        CustomService service = new CustomService(this,
                deploymentConfiguration);
        service.init();
        return service;
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        super.service(request, new SlowResponse(response));
    }

    public static void slowDownResponse() {
        CurrentInstance.set(SlowResponseFlag.class, new SlowResponseFlag(500));
    }

    private record SlowResponseFlag(int timeout) {
    }

    private static class SlowResponse extends HttpServletResponseWrapper {

        public SlowResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            var slowResponseFlag = CurrentInstance.get(SlowResponseFlag.class);
            if (slowResponseFlag != null) {
                try {
                    Thread.sleep(slowResponseFlag.timeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            return super.getOutputStream();
        }
    }
}
