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
package com.vaadin.flow.server;

import java.io.IOException;
import java.io.Writer;

/**
 * A {@link RequestHandler} that presents an informative page if the browser in
 * use is unsupported.
 *
 * @since 1.0
 */
public class UnsupportedBrowserHandler extends SynchronizedRequestHandler {

    /** Cookie used to ignore browser checks. */
    public static final String FORCE_LOAD_COOKIE = "vaadinforceload=1";

    public static final String CLOSING_BRACKET = "    }";

    private static final String UNSUPPORTED_PAGE_HEAD_CONTENT = "<head>"
            + "  <style>"
            + "    html {"
            + "      background: #fff;"
            + "      color: #444;"
            + "      font: 400 1em/1.5 \"Helvetica Neue\", Roboto, \"Segoe UI\", sans-serif;"
            + "      padding: 2em;"
            + CLOSING_BRACKET
            + "    body {"
            + "      margin: 2em auto;"
            + "      width: 28em;"
            + "      max-width: 100%;"
            + CLOSING_BRACKET
            + "    h1 {"
            + "      line-height: 1.1;"
            + "      margin: 2em 0 1em;"
            + "      color: #000;"
            + "      font-weight: 400;"
            + CLOSING_BRACKET
            + "    p {"
            + "      margin: 0.5em 0 0;"
            + CLOSING_BRACKET
            + "    a {"
            + "      text-decoration: none;"
            + "      color: #007df0;"
            + CLOSING_BRACKET
            + "    sub {"
            + "      display: block;"
            + "      margin-top: 2.5em;"
            + "      text-align: center;"
            + "      border-top: 1px solid #eee;"
            + "      padding-top: 2em;"
            + CLOSING_BRACKET
            + "    sub,"
            + "    small {"
            + "      color: #999;"
            + CLOSING_BRACKET
            + "  </style>"
            + "</head>";

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
                                             VaadinRequest request, VaadinResponse response) throws IOException {

        // Check if the browser is supported
        WebBrowser browser = session.getBrowser();
        final String cookie = request.getHeader("Cookie");
        if (browser.isTooOldToFunctionProperly()) {
            // bypass if cookie set
            if (cookie == null || !cookie.contains(FORCE_LOAD_COOKIE)) {
                writeBrowserTooOldPage(request, response);
                return true; // request handled
            }
        }
        // check for trying to run ie11 in development mode
        if (browser.isIE() && !session.getConfiguration().isProductionMode() && session.getConfiguration().isCompatibilityMode()) {
            // bypass if cookie set
            if (cookie == null || !cookie.contains(FORCE_LOAD_COOKIE)) {
                writeIE11InDevelopmentModePage(response);
                return true;
            }
        }

        return false; // pass to next handler
    }

    /**
     * Writes a page encouraging the user to upgrade to a more current browser.
     *
     * @param request  The request to handle
     * @param response The response object to which a response can be written.
     * @throws IOException if an IO error occurred
     */
    protected void writeBrowserTooOldPage(VaadinRequest request,
                                          VaadinResponse response) throws IOException {
        Writer page = response.getWriter();
        WebBrowser browser = VaadinSession.getCurrent().getBrowser();

        // @formatter:off
        page.write(
                "<html>"
                        + UNSUPPORTED_PAGE_HEAD_CONTENT
                        + "<body><h1>I'm sorry, but your browser is not supported</h1>"
                        + "<p>The version (" + browser.getBrowserMajorVersion()
                        + "." + browser.getBrowserMinorVersion()
                        + ") of the browser you are using "
                        + " is outdated and not supported.</p>"
                        + "<p>You should <b>consider upgrading</b> to a more up-to-date browser.</p> "
                        + "<p>The most popular browsers are <b>"
                        + " <a href=\"https://www.google.com/chrome\">Chrome</a>,"
                        + " <a href=\"http://www.mozilla.com/firefox\">Firefox</a>,"
                        + (browser.isWindows()
                                ? " <a href=\"https://www.microsoft.com/en-us/windows/microsoft-edge\">Edge</a>,"
                                : "")
                        + " <a href=\"http://www.opera.com/browser\">Opera</a>"
                        + " and <a href=\"http://www.apple.com/safari\">Safari</a>.</b><br/>"
                        + "Upgrading to the latest version of one of these <b>will make the web safer, faster and better looking.</b></p>"
                        + "<p><sub><a onclick=\"document.cookie='"
                        + FORCE_LOAD_COOKIE
                        + "';window.location.reload();return false;\" href=\"#\">Continue without updating</a> (not recommended)</sub></p>"
                        + "</body>\n" + "</html>");
        // @formatter:on

        page.close();
    }

    /**
     * Writes a page that explains that Production Mode is required for Internet Explorer 11 to work.
     *
     * @param response the response object to write response to
     * @throws IOException if an IO error occurred
     */
    private void writeIE11InDevelopmentModePage(VaadinResponse response) throws IOException {
        Writer page = response.getWriter();

        // @formatter:off
        page.write(
                "<html>"
                        + UNSUPPORTED_PAGE_HEAD_CONTENT
                        + "<body style=\"width:34em;\"><h1>Internet Explorer 11 requires Vaadin Flow to be run in production mode.</h1>"
                        + "<p>To test your app with IE11, you need make a production build and run the app in production mode.</p>"
                        + "<p>The production build is made with e.g. a Maven profile that adds the <code>flow-server-production-mode</code> "
                        + "dependency and executes the following goals for the <code>com.vaadin:vaadin-maven-plugin</code>"
                        + "<ul><li><code>copy-production-files</code></li>"
                        + "<li><code>package-for-production</code></li></ul></p>"
                        + "<p>The production mode can be enabled by setting the <code>vaadin.productionMode=true</code> "
                        + "property for the deployment configuration using an application or a system property.<p>"
                        + "<p>You can find more information about the production mode from "
                        + "<a href=\"https://vaadin.com/docs/flow/production/tutorial-production-mode-basic.html\">documentation</a>.</p>"
                        + "<p><sub><a onclick=\"document.cookie='"
                        + FORCE_LOAD_COOKIE
                        + "';window.location.reload();return false;\" href=\"#\">Continue anyway<br>" +
                        "(eg. if you've setup ES5 transpilation of frontend resources without running Flow in production mode)</sub></p>"
                        + "</body>\n"
                        + "</html>");
        // @formatter:on

        page.close();
    }
}
