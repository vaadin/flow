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
package com.vaadin.flow.test.cspstrict;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Records every Content Security Policy violation the browser reports, so that
 * the integration tests can read them back from
 * {@code window.Vaadin.Flow.cspViolations}.
 * <p>
 * The listener is an inline script added to the head of the bootstrap page from
 * an {@code IndexHtmlRequestListener}. It has to be inline, and first in the
 * document: a module loaded with the bundle would miss the violations reported
 * while the page is still being parsed. Vaadin puts the nonce of the response
 * on the script, as on every other script of the page, which is also what makes
 * this a check of the policy hook itself.
 */
@Component
public class CspViolationCollector implements VaadinServiceInitListener {

    /**
     * Name of the global array holding the recorded violations, each with the
     * directive, the blocked URI, the source location and a sample of the
     * blocked content.
     */
    public static final String VIOLATIONS = "window.Vaadin.Flow.cspViolations";

    private static final String SCRIPT = """
            window.Vaadin = window.Vaadin || {};
            window.Vaadin.Flow = window.Vaadin.Flow || {};
            window.Vaadin.Flow.cspViolations = [];
            document.addEventListener('securitypolicyviolation', function (event) {
              window.Vaadin.Flow.cspViolations.push({
                directive: event.effectiveDirective,
                blockedUri: event.blockedURI,
                source: event.sourceFile + ':' + event.lineNumber,
                sample: event.sample
              });
            });
            """;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> {
            Element script = response.getDocument().head()
                    .prependElement("script");
            script.appendChild(new DataNode(SCRIPT));
        });
    }
}
