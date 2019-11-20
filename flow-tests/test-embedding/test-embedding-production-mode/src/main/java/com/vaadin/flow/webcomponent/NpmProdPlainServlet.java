/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import javax.servlet.annotation.WebServlet;
import java.io.PrintWriter;
import java.util.function.Consumer;

import com.vaadin.flow.webcomponent.servlets.AbstractPlainServlet;

// npm mode is able to survive a root-mapped servlet, while compatibility
// mode is not
@WebServlet(urlPatterns = { "/*", "/items/*" }, asyncSupported = true)
public class NpmProdPlainServlet extends AbstractPlainServlet {
    @Override
    protected Consumer<PrintWriter> getImportsWriter() {
        return this::writeNpmImports;
    }
}
