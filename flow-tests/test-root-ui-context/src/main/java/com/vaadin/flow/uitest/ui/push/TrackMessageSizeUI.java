package com.vaadin.flow.uitest.ui.push;

import javax.servlet.ServletContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;

public class TrackMessageSizeUI extends AbstractTestUIWithLog {

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
            + "        window.logToServer('Test failed, see javascript console for details.');\n"
            + "    }" + "}\n";

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        String methodImplementation = findMethodImplementation();

        getPage().addJavaScript("VAADIN/static/push/vaadinPush.js");
        getPage().executeJs(
                "window.logToServer=function(msg){ $0.$server.log(msg)};",
                this);

        getPage()
                .executeJs(methodImplementation + testMethod + buildTestCase());
    }

    @Override
    @ClientCallable
    protected void log(String msg) {
        super.log(msg);
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
                + "window.logToServer('All tests run')\n";
    }

    private String findMethodImplementation() {
        String filename = "/VAADIN/static/push/vaadinPush.js";
        URL resourceURL = findResourceURL(filename,
                (VaadinServletService) VaadinService.getCurrent());
        if (resourceURL == null) {
            log("Can't find " + filename);
            return null;
        }

        try {
            String string = IOUtils.toString(resourceURL,
                    StandardCharsets.UTF_8);

            // Find the function inside the script content
            int startIndex = string.indexOf("function _trackMessageSize");
            if (startIndex == -1) {
                log("function not found");
                return null;
            }

            // Assumes there's a /** comment before the next function
            int endIndex = string.indexOf("/**", startIndex);
            if (endIndex == -1) {
                log("End of function not found");
                return null;
            }

            return string.substring(startIndex, endIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL findResourceURL(String filename, VaadinServletService service) {
        ServletContext sc = service.getServlet().getServletContext();
        URL resourceUrl;
        try {
            resourceUrl = sc.getResource(filename);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        if (resourceUrl == null) {
            // try if requested file is found from classloader

            // strip leading "/" otherwise stream from JAR wont work
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }

            resourceUrl = service.getClassLoader().getResource(filename);
        }
        return resourceUrl;
    }

}