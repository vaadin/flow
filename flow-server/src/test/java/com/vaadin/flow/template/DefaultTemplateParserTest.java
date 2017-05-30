/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.template;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.template.PolymerTemplateTest.ModelClass;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.util.CurrentInstance;

public class DefaultTemplateParserTest {

    @Tag("foo")
    @HtmlImport("bar.html")
    @HtmlImport("bar1.html")
    private static class ImportsInspectTemplate
            extends PolymerTemplate<ModelClass> {

    }

    @Test
    public void defaultParser_returnsContent() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        WrappedHttpSession wrappedSession = Mockito
                .mock(WrappedHttpSession.class);

        Mockito.when(request.getWrappedSession()).thenReturn(wrappedSession);

        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinSession.class, session);

        DefaultTemplateParser parser = new DefaultTemplateParser();

    }
}
