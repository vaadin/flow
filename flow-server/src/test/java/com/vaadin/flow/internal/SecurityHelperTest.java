/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.shared.ApplicationConstants;

public class SecurityHelperTest {

    @Test
    public void isInternalRequest_externalRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Assert.assertFalse(SecurityHelper.isInternalRequest(request));
    }

    @Test
    public void isInternalRequest_serviceWorkerInitiated() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("referer")).thenReturn("/sw.js");
        Assert.assertTrue(SecurityHelper.isInternalRequest(request));
    }

    @Test
    public void isInternalRequest_hasVaadinRequestType() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.UIDL.getIdentifier());
        Mockito.when(request.getHeader("referer")).thenReturn("/sw.js");
        Assert.assertTrue(SecurityHelper.isInternalRequest(request));
    }
}
