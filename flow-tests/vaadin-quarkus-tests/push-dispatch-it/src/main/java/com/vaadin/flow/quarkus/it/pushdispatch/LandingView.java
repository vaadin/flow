/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.pushdispatch;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Landing route that lets the IT (and a human in dev mode) establish a Vaadin
 * session and Push websocket before triggering the blocking navigation. The
 * {@link RouterLink} goes through Vaadin's in-app router, which (with
 * {@code @Push(WEBSOCKET)}) sends the UIDL navigation request over the existing
 * Push websocket on the event loop — exactly the path that exercises the
 * dispatch-to-worker setting.
 * <p>
 * The on-page text explains what the user is reproducing, which is useful when
 * running {@code mvn quarkus:dev} against this module manually.
 */
@Route("")
public class LandingView extends Div {

    public static final String LINK_ID = "link-to-push-blocking";

    public LandingView() {
        add(new Html(
                """
                        <div>
                          <h1>Vaadin Quarkus — Push dispatch deadlock repro</h1>
                          <p>This module reproduces the deadlock that the Vaadin Quarkus
                             extension's default
                             <code>quarkus.websocket.dispatch-to-worker=true</code>
                             protects against. With <code>@Push(WEBSOCKET)</code> the
                             Vaadin navigation request flows over the Push websocket on
                             the Vert.x event loop, so an <code>afterNavigation</code>
                             hook that calls a synchronous Quarkus REST client ends up
                             running blocking code on the event loop.</p>
                          <p>Click the link below to navigate to
                             <code>/push-blocking</code>, whose
                             <code>afterNavigation</code> calls the REST endpoint at
                             <code>/api/slow</code> (sleeps 5s, returns "ready").</p>
                          <h1>Expected result</h1>
                          <p>With the extension's default
                             (<code>quarkus.websocket.dispatch-to-worker=true</code>) the
                             navigation runs on a worker thread, the REST call returns
                             normally, and the next page shows <code>%s</code>.</p>
                          <p>With the Quarkus default
                             (<code>dispatch-to-worker=false</code>) the navigation runs
                             on the Vert.x event loop. RESTEasy Reactive refuses to block
                             on that thread and throws
                             <code>BlockingNotAllowedException</code>; Vaadin routes to
                             <code>PushBlockingErrorView</code>, which shows
                             <code>%s</code> plus the full server-side stack trace.</p>
                          <p>Toggle the failure mode by uncommenting
                             <code>quarkus.websocket.dispatch-to-worker=false</code> in
                             <code>application.properties</code> and restarting.</p>
                          <hr/>
                        </div>
                        """
                        .formatted(PushBlockingAfterNavView.READY_TEXT,
                                PushBlockingErrorView.ERROR_TEXT)));

        RouterLink link = new RouterLink("Go to /push-blocking",
                PushBlockingAfterNavView.class);
        link.setId(LINK_ID);
        add(link);
    }

}
