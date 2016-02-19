/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.namespace;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;

public class ReconnectDialogConfigurationNamespaceTest extends
        AbstractMapNamespaceTest<ReconnectDialogConfigurationNamespace> {

    private StateNode node = new StateNode(
            ReconnectDialogConfigurationNamespace.class);
    private final ReconnectDialogConfigurationNamespace namespace = new ReconnectDialogConfigurationNamespace(
            node);

    @Test
    public void defaults() {
        Assert.assertEquals(
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_DEFAULT,
                namespace.getDialogText());
        Assert.assertEquals(
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_DEFAULT,
                namespace.getDialogTextGaveUp());
        Assert.assertEquals(
                ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_DEFAULT,
                namespace.getReconnectAttempts());
        Assert.assertEquals(
                ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_DEFAULT,
                namespace.getReconnectInterval());
        Assert.assertEquals(
                ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_DEFAULT,
                namespace.getDialogGracePeriod());
        Assert.assertEquals(
                ReconnectDialogConfigurationNamespace.DIALOG_MODAL_DEFAULT,
                namespace.isDialogModal());
    }

    @Test
    public void setGetDialogText() {
        testString(namespace,
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_KEY,
                namespace::setDialogText, namespace::getDialogText);
    }

    @Test
    public void setGetDialogTextGaveUp() {
        testString(namespace,
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_KEY,
                namespace::setDialogTextGaveUp, namespace::getDialogTextGaveUp);
    }

    @Test
    public void setGetReconnectAttempts() {
        testInt(namespace,
                ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_KEY,
                namespace::setReconnectAttempts,
                namespace::getReconnectAttempts);
    }

    @Test
    public void setGetReconnectInterval() {
        testInt(namespace,
                ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_KEY,
                namespace::setReconnectInterval,
                namespace::getReconnectInterval);
    }

    @Test
    public void setGetDialogGracePeriod() {
        testInt(namespace,
                ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_KEY,
                namespace::setDialogGracePeriod,
                namespace::getDialogGracePeriod);

    }

    @Test
    public void setGetDialogModal() {
        testBoolean(namespace,
                ReconnectDialogConfigurationNamespace.DIALOG_MODAL_KEY,
                namespace::setDialogModal, namespace::isDialogModal);

    }

}
