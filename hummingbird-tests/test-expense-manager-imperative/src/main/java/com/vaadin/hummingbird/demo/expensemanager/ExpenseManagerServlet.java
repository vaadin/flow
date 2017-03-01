/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.demo.expensemanager;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.demo.expensemanager.ExpenseManagerServlet.RouterConf;
import com.vaadin.hummingbird.demo.expensemanager.domain.ImageService;
import com.vaadin.hummingbird.demo.expensemanager.views.EditView;
import com.vaadin.hummingbird.demo.expensemanager.views.MainLayout;
import com.vaadin.hummingbird.demo.expensemanager.views.OverView;
import com.vaadin.hummingbird.router.InternalRedirectHandler;
import com.vaadin.hummingbird.router.Location;
import com.vaadin.hummingbird.router.RouterConfiguration;
import com.vaadin.hummingbird.router.RouterConfigurator;
import com.vaadin.server.VaadinServlet;

/**
 * Servlet for the application.
 */
@WebServlet(urlPatterns = "/*", name = "ExpenseManagerServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = ExpenseManagerUI.class, routerConfigurator = RouterConf.class, productionMode = false)
public class ExpenseManagerServlet extends VaadinServlet {

    /**
     * Router configuration for the demo.
     */
    public static class RouterConf implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            configuration.setRoute("",
                    new InternalRedirectHandler(new Location("overview")));
            configuration.setRoute("overview", OverView.class,
                    MainLayout.class);
            configuration.setRoute("expense/{id}", EditView.class,
                    MainLayout.class);
        }
    }

    @WebServlet("/upload")
    @MultipartConfig
    public static class UploadServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest request,
                HttpServletResponse response)
                throws ServletException, IOException {
            String imageData = ImageService.INSTANCE
                    .partToData(request.getPart("file"));

            request.getSession().setAttribute("receipt-upload", imageData);
        }
    }
}
