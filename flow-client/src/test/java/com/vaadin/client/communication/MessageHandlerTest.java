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
package com.vaadin.client.communication;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.flow.collection.JsArray;

public class MessageHandlerTest {

    @Test
    public void messageWithMultipleJson_messagesAreSplit() {
        String sampleMessage = "for(;;);[{\"syncId\":216,\"clientId\":0,\"meta\":{\"async\":true},\"timings\":[725,1]}]for(;;);[{\"syncId\":217,\"clientId\":0,\"meta\":{\"async\":true},\"timings\":[726,1]}]";
        JsArray<String> array = MessageHandler
                .splitMultipleMessages(sampleMessage);
        Assert.assertEquals(2, array.length());
        Assert.assertEquals(
                "for(;;);[{\"syncId\":216,\"clientId\":0,\"meta\":{\"async\":true},\"timings\":[725,1]}]",
                array.get(0));
        Assert.assertEquals(
                "for(;;);[{\"syncId\":217,\"clientId\":0,\"meta\":{\"async\":true},\"timings\":[726,1]}]",
                array.get(1));
    }

    @Test
    public void messageWithSingleJson_messageIsTheSame() {
        String sampleMessage = "for(;;);[{\"syncId\":216,\"clientId\":0,\"meta\":{\"async\":true},\"timings\":[725,1]}]";
        JsArray<String> array = MessageHandler
                .splitMultipleMessages(sampleMessage);
        Assert.assertEquals(1, array.length());
        Assert.assertEquals(sampleMessage, array.get(0));
    }

    @Test
    public void invalidMessage_arrayIsEmpty() {
        String sampleMessage = "invalidMessage";
        JsArray<String> array = MessageHandler
                .splitMultipleMessages(sampleMessage);
        Assert.assertEquals(0, array.length());
    }
}
