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
package com.vaadin.flow.spring.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.ApplicationConstants;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UidlRedirectStrategyTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpServletRequest request;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpServletResponse response;

    private UidlRedirectStrategy strategy;

    @BeforeEach
    public void setup() {
        strategy = new UidlRedirectStrategy();
        when(request.getHttpServletMapping().getPattern()).thenReturn("/");
    }

    @AfterEach
    public void cleanup() {
        CurrentInstance.clearAll();
    }

    @Test
    public void isInternalRequest_setPageLocation()
            throws IOException, ServletException {
        when(request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(ApplicationConstants.REQUEST_TYPE_UIDL);

        var ui = mock(UI.class, Answers.RETURNS_DEEP_STUBS);
        CurrentInstance.set(UI.class, ui);

        var page = mock(Page.class);
        when(ui.getPage()).thenReturn(page);

        strategy.sendRedirect(request, response, "/foo");

        verify(page).setLocation("/foo");
    }

    @Test
    public void isExternalRequest_useDefaultRedirect()
            throws IOException, ServletException {
        when(request.getContextPath()).thenReturn("");
        when(response.encodeRedirectURL(anyString()))
                .thenAnswer(i -> i.getArguments()[0]);

        strategy.sendRedirect(request, response, "/foo");

        verify(response).sendRedirect("/foo");
    }
}
