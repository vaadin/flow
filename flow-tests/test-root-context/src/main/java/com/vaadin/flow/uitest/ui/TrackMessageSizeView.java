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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Feeds length-prefixed chunks through the message parsing of the bundled
 * atmosphere.js client and checks the messages it reassembles.
 */
@Route("com.vaadin.flow.uitest.ui.TrackMessageSizeView")
@Push(transport = Transport.LONG_POLLING)
public class TrackMessageSizeView extends Div {

    public static final String LOG_ELEMENT_ID = "logId";

    private static final String TEST_METHOD = """
            function testSequence(atmosphere, expected, data) {
                var protocol = new atmosphere.AtmosphereProtocol();
                var request = {trackMessageLength: true, messageDelimiter: '|'};
                var messages = [];
                for (var i = 0; i < data.length; i++) {
                    var result = protocol.processMessage(data[i], request);
                    if (result) {
                        messages = messages.concat(result.messages);
                    }
                }
                if (JSON.stringify(expected) != JSON.stringify(messages)) {
                    console.error('Expected', expected, 'but got', messages, 'for', data);
                    $0.innerHTML = 'Test failed, see javascript console for details.';
                }
            }
            """;

    private static final String TEST_CASES = """
            function runTests(atmosphere) {
                testSequence(atmosphere, ['a', 'b'], ['1|a1|b', '']);
                testSequence(atmosphere, ['a', 'b'], ['1|a1|', 'b']);
                testSequence(atmosphere, ['a', 'b'], ['1|a1', '|b']);
                testSequence(atmosphere, ['a', 'b'], ['1|a', '1|b']);
                testSequence(atmosphere, ['a', 'b'], ['1|a', '', '1|b']);
                testSequence(atmosphere, ['a|', '|b'], ['2|a|2||b']);
                testSequence(atmosphere, ['a|', 'b'], ['2|a|', '', '1|b']);
                testSequence(atmosphere, ['a|', 'b'], ['2|a|', '1|b']);
                testSequence(atmosphere, ['a|', 'b'], ['2|a|1', '|b']);
                testSequence(atmosphere, ['a|', 'b'], ['2|a|1|', 'b']);
                testSequence(atmosphere, [' ', 'b'], ['1| 1|b']);
                testSequence(atmosphere, [' ', 'b'], ['1| ','1|b']);
                testSequence(atmosphere, [' ', 'b'], ['1|',' 1|b']);
                if ($0.innerHTML === '') {
                    $0.innerHTML = 'All tests run';
                }
            }
            // vaadinPush.js is loaded on demand
            (function waitForPush() {
                var atmosphere = window.vaadinPush && window.vaadinPush.atmosphere;
                if (atmosphere) {
                    runTests(atmosphere);
                } else {
                    setTimeout(waitForPush, 50);
                }
            })();
            """;

    public TrackMessageSizeView() {
        Element logElement = ElementFactory.createDiv().setAttribute("id",
                LOG_ELEMENT_ID);
        getElement().appendChild(logElement);

        logElement.getNode().runWhenAttached(ui -> ui.getPage()
                .executeJs(TEST_METHOD + TEST_CASES, logElement));
    }

}
