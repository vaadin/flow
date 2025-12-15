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
package com.vaadin.flow.i18n;

import java.net.URI;
import java.util.Set;

import org.junit.Test;

import com.vaadin.flow.hotswap.HotswapResourceEvent;
import com.vaadin.flow.hotswap.UIUpdateStrategy;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class DefaultTranslationsHotswapperTest {

    DefaultTranslationsHotswapper hotswapper = new DefaultTranslationsHotswapper();
    VaadinService service = new MockVaadinServletService(false);

    @Test
    public void defaultI18nFolder_propertyFileChange_sendHrmEventAndRefreshUI() {
        HotswapResourceEvent event = spy(
                new HotswapResourceEvent(service, Set.of(URI.create(
                        "file://some/path/vaadin-i18n/translation.properties"))));
        hotswapper.onResourcesChange(event);

        assertFalse("Page reload is not necessary",
                event.anyUIRequiresPageReload());

        UIUpdateStrategy updateStrategy = event
                .getUIUpdateStrategy(new MockUI()).orElse(null);
        assertEquals("Should refresh all UIs", UIUpdateStrategy.REFRESH,
                updateStrategy);

        verify(event).sendHmrEvent(eq("translations-update"), any());
    }

    @Test
    public void nonDefaultI18fFolder_propertyFileChanged_ignore() {
        HotswapResourceEvent event = spy(
                new HotswapResourceEvent(service, Set.of(URI.create(
                        "file://some/path/resources/translation.properties"))));
        hotswapper.onResourcesChange(event);

        assertFalse("Page reload is not necessary",
                event.anyUIRequiresPageReload());
        assertTrue("Should not refresh UIs",
                event.getUIUpdateStrategy(new MockUI()).isEmpty());

        verify(event, never()).sendHmrEvent(eq("translations-update"), any());
    }

    @Test
    public void defaultI18fFolder_nonPropertyFileChanged_ignore() {
        HotswapResourceEvent event = spy(
                new HotswapResourceEvent(service, Set.of(URI.create(
                        "file://some/path/vaadin-i18n/translation.txt"))));
        hotswapper.onResourcesChange(event);

        assertFalse("Page reload is not necessary",
                event.anyUIRequiresPageReload());

        assertTrue("Should not refresh UIs",
                event.getUIUpdateStrategy(new MockUI()).isEmpty());

        verify(event, never()).sendHmrEvent(eq("translations-update"), any());
    }

}
