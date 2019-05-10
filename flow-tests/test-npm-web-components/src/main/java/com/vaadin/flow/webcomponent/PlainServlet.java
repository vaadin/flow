/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.webcomponent;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = { "/items/*"}, asyncSupported = true)
public class PlainServlet extends HttpServlet {
    public PlainServlet() {
        System.clearProperty("vaadin.bowerMode");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head>");
            out.println(
                    "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
            out.println(
                    "<link rel='import' href='/vaadin/web-component/client-select.html'>");
            out.println("<title>Embedded web component</title></head>");
            out.println("<body>");
            out.println(
                    "<client-select show='true'>Web Component</client-select>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
