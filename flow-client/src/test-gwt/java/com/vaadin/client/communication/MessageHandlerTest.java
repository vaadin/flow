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
package com.vaadin.client.communication;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.ValueMap;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MessageHandlerTest extends ClientEngineTestBase{

    @Test
    public void unwrapValidJson() {
        MessageHandler messageHandler = Mockito.mock(MessageHandler.class);
        ValueMap json = (ValueMap)JavaScriptObject.createObject();
        
        Mockito.doCallRealMethod().when(messageHandler).handleJSON(Mockito.any());
        messageHandler.handleJSON((ValueMap)json);
        Assert.assertTrue(true);
    }
}
