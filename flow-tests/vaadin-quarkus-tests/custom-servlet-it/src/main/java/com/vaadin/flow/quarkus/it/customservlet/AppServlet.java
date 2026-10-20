/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.customservlet;

import jakarta.servlet.annotation.WebServlet;

import com.vaadin.quarkus.QuarkusVaadinServlet;

/**
 * The only Vaadin servlet of the application. Because it exists, the extension
 * does not register its default servlet on {@code /*}, so everything Vaadin
 * serves has to work under the {@code /app} prefix.
 */
@WebServlet(urlPatterns = "/app/*", asyncSupported = true, loadOnStartup = 1)
public class AppServlet extends QuarkusVaadinServlet {
}
