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
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.server.communication.AtmospherePushConnection.FragmentedMessage;
import com.vaadin.flow.shared.communication.PushConstants;

public class FragmentedMessageTest {

    @Test
    public void shortMessageCompleteImmediately() throws IOException {
        FragmentedMessage msg = new FragmentedMessage();
        Assert.assertTrue(
                msg.append(new StringReader("Hello".length() + "|" + "Hello")));
        Assert.assertEquals("Hello", IOUtils.toString(msg.getReader()));
    }

    @Test
    public void longMessageConcatenated() throws IOException {
        FragmentedMessage msg = new FragmentedMessage();
        String text = "HelloWorld".repeat(1700);
        String textWithLength = text.length() + "|" + text;
        String part1 = textWithLength.substring(0,
                PushConstants.WEBSOCKET_BUFFER_SIZE);
        String part2 = textWithLength
                .substring(PushConstants.WEBSOCKET_BUFFER_SIZE);

        Assert.assertEquals(PushConstants.WEBSOCKET_BUFFER_SIZE,
                part1.length());
        Assert.assertEquals(
                textWithLength.length() - PushConstants.WEBSOCKET_BUFFER_SIZE,
                part2.length());

        StringReader messageReader = new StringReader(part1);
        Assert.assertFalse(msg.append(messageReader));
        StringReader messageReader2 = new StringReader(part2);
        Assert.assertTrue(msg.append(messageReader2));
        Assert.assertEquals(text, IOUtils.toString(msg.getReader()));
    }

    @Test
    public void lengthEqualsLimitHandledCorrectly() throws IOException {
        FragmentedMessage msg = new FragmentedMessage();
        int length = (PushConstants.WEBSOCKET_BUFFER_SIZE
                - String.valueOf(PushConstants.WEBSOCKET_BUFFER_SIZE).length()
                - 1);
        String text = "A".repeat(length);
        String textWithLength = length + "|" + text;

        Assert.assertEquals(PushConstants.WEBSOCKET_BUFFER_SIZE,
                textWithLength.length());

        StringReader messageReader = new StringReader(textWithLength);
        Assert.assertTrue(msg.append(messageReader));
        Assert.assertEquals(text, IOUtils.toString(msg.getReader()));
    }
}
