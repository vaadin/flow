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
package com.vaadin.flow.server;

import org.junit.Assert;
import org.junit.Test;

public class SystemMessagesTest {

    @Test
    public void syncError_defaultValues() {
        SystemMessages messages = new CustomizedSystemMessages();

        Assert.assertNull("Default URL should be null",
                messages.getSyncErrorURL());
        Assert.assertTrue("Default notification should be enabled",
                messages.isSyncErrorNotificationEnabled());
        Assert.assertEquals("Default caption should match",
                "Synchronization Error", messages.getSyncErrorCaption());
        Assert.assertEquals("Default message should match",
                "Your session needs to be refreshed. Click here or press ESC to reload and restore your last saved state.",
                messages.getSyncErrorMessage());
    }

    @Test
    public void syncError_notificationEnabled_returnsCaptionAndMessage() {
        SystemMessages messages = new CustomizedSystemMessages();

        // By default, notification is enabled
        Assert.assertTrue(messages.isSyncErrorNotificationEnabled());
        Assert.assertNotNull(messages.getSyncErrorCaption());
        Assert.assertNotNull(messages.getSyncErrorMessage());
    }

    @Test
    public void customizedSyncError_notificationDisabled_returnsNullCaptionAndMessage() {
        CustomizedSystemMessages messages = new CustomizedSystemMessages();

        messages.setSyncErrorNotificationEnabled(false);
        messages.setSyncErrorCaption("Custom Caption");
        messages.setSyncErrorMessage("Custom message text");

        Assert.assertFalse("Notification should be disabled",
                messages.isSyncErrorNotificationEnabled());
        Assert.assertNull(
                "Caption should be null when notification is disabled",
                messages.getSyncErrorCaption());
        Assert.assertNull(
                "Message should be null when notification is disabled",
                messages.getSyncErrorMessage());
    }

    @Test
    public void customizedSyncError_urlNotAffectedByNotificationEnabled() {
        CustomizedSystemMessages messages = new CustomizedSystemMessages();

        messages.setSyncErrorURL("/redirect-url");
        messages.setSyncErrorNotificationEnabled(false);

        // URL should still be returned even when notification is disabled
        Assert.assertEquals("/redirect-url", messages.getSyncErrorURL());
    }
}
