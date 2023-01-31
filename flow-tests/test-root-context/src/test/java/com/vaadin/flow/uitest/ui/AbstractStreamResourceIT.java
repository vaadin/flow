/*
  * Copyright 2000-2023 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractStreamResourceIT extends ChromeBrowserTest {

    /*
     * Stolen from stackexchange.
     *
     * It's not possible to use a straight way to download the link externally
     * since it will use another session and the link will be invalid in this
     * session. So either this pure client side way or external download with
     * cookies copy (which allows preserve the session) needs to be used.
     */
    public InputStream download(String url) throws IOException {
        String script = "var url = arguments[0];"
                + "var callback = arguments[arguments.length - 1];"
                + "var xhr = new XMLHttpRequest();"
                + "xhr.open('GET', url, true);"
                + "xhr.responseType = \"arraybuffer\";" +
                // force the HTTP response, response-type header to be array
                // buffer
                "xhr.onload = function() {"
                + "  var arrayBuffer = xhr.response;"
                + "  var byteArray = new Uint8Array(arrayBuffer);"
                + "  callback(byteArray);" + "};" + "xhr.send();";
        Object response = ((JavascriptExecutor) getDriver())
                .executeAsyncScript(script, url);
        // Selenium returns an Array of Long, we need byte[]
        ArrayList<?> byteList = (ArrayList<?>) response;
        byte[] bytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            Long byt = (Long) byteList.get(i);
            bytes[i] = byt.byteValue();
        }
        return new ByteArrayInputStream(bytes);
    }
}
