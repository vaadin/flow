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
package com.vaadin.flow.server.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinResponse;

public class WebComponentBootstrapHandlerTest {

    @Test
    public void writeBootstrapPage_skipMetaAndStyleHeaderElements()
            throws IOException {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").normalise().head();
        Element meta = head.ownerDocument().createElement("meta");
        head.appendChild(meta);
        meta.attr("http-equiv", "Content-Type");

        Element style = head.ownerDocument().createElement("style");
        head.appendChild(style);
        style.attr("type'", "text/css");
        style.text("body {height:100vh;width:100vw;margin:0;}");

        Element script = head.ownerDocument().createElement("script");
        head.appendChild(script);
        script.text("var i=1;");

        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(response.getOutputStream()).thenReturn(stream);
        handler.writeBootstrapPage("", response, head, "");

        String resultingScript = stream.toString();

        Assert.assertThat(resultingScript,
                CoreMatchers.containsString("var i=1;"));
        Assert.assertThat(resultingScript, CoreMatchers.not(CoreMatchers
                .containsString("body {height:100vh;width:100vw;margin:0;}")));
        Assert.assertThat(resultingScript,
                CoreMatchers.not(CoreMatchers.containsString("http-equiv")));
    }
}
