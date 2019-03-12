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
 *
 */

package com.vaadin.flow.uitest.ui;

import javax.servlet.ServletContext;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

@Route("com.vaadin.flow.uitest.ui.TrackMessageSizeView")
public class TrackMessageSizeView extends Div {

    public static final String LOG_ELEMENT_ID = "logId";

    private Element logElement;

    private String testMethod = "function testSequence(expected, data) {\n"
            + "    var request = {trackMessageLength: true, messageDelimiter: '|'};\n"
            + "    _request = {trackMessageLength: true, messageDelimiter: '|'};\n"
            + "    _handleProtocol = function(a,message) {return message;};"
            + "    var response = {partialMessage: ''};\n"
            + "    var messages = [];\n"
            + "    for (var i = 0; i < data.length; i++) {\n"
            + "        if (!_trackMessageSize(data[i], request, response))\n"
            + "            messages = messages.concat(response.messages);\n"
            + "    }\n"
            + "    if (JSON.stringify(expected) != JSON.stringify(messages)) {\n"
            + "        if (console && typeof console.error == 'function') console.error('Expected', expected, 'but got', messages, 'for', data);\n"
            + "        $0.innerHTML = 'Test failed, see javascript console for details.';\n"
            + "    }" + "}\n";


    public TrackMessageSizeView() {

        logElement = ElementFactory.createDiv().setAttribute("id",
                LOG_ELEMENT_ID);
        getElement().appendChild(logElement);

        String methodImplementation = findMethodImplementation();

        logElement.getNode()
                .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                        methodImplementation + testMethod + buildTestCase(), logElement));

    }

    private String buildTestCase() {
        // Could maybe express the cases in java and generate JS?
        return "testSequence(['a', 'b'], ['1|a1|b', '']);\n"
                + "testSequence(['a', 'b'], ['1|a1|', 'b']);\n"
                + "testSequence(['a', 'b'], ['1|a1', '|b']);\n"
                + "testSequence(['a', 'b'], ['1|a', '1|b']);\n"
                + "testSequence(['a', 'b'], ['1|a', '', '1|b']);\n"
                + "testSequence(['a|', '|b'], ['2|a|2||b']);\n"
                + "testSequence(['a|', 'b'], ['2|a|', '', '1|b']);\n"
                + "testSequence(['a|', 'b'], ['2|a|', '1|b']);\n"
                + "testSequence(['a|', 'b'], ['2|a|1', '|b']);\n"
                + "testSequence(['a|', 'b'], ['2|a|1|', 'b']);\n"
                + "testSequence([' ', 'b'], ['1| 1|b']);\n"
                + "testSequence([' ', 'b'], ['1| ','1|b']);\n"
                + "testSequence([' ', 'b'], ['1|',' 1|b']);\n"
                + "if($0.innerHTML === '') { \n"
                + "    $0.innerHTML = 'All tests run'; "
                + "}\n";
    }

    private String findMethodImplementation() {
        String filename = "/VAADIN/static/push/vaadinPush.js";

        String content = getFileContent(filename, (VaadinServletService) VaadinService.getCurrent());

        if (content == null) {
            log("Can't find " + filename);
            return null;
        }

        // Find the function inside the script content
        int startIndex = content.indexOf("function _trackMessageSize");
        if (startIndex == -1) {
            log("function not found");
            return null;
        }

        // Assumes there's a /** comment before the next function
        int endIndex = content.indexOf("/**", startIndex);
        if (endIndex == -1) {
            log("End of function not found");
            return null;
        }

        content = content.substring(startIndex, endIndex);
        content = content.replaceAll("jQuery", "jQueryVaadin");
        return content;

    }

    private String getFileContent(String filename, VaadinServletService service) {
        ServletContext sc = service.getServlet().getServletContext();

        try(InputStream inputStream = sc.getResourceAsStream(filename)) {
            return inputStream != null ? IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()) : null;
        } catch ( IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void log(String log){
        logElement.setText(log);
    }

}
