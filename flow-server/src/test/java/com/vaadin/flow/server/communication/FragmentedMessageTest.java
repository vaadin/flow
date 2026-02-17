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
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.communication.AtmospherePushConnection.FragmentedMessage;
import com.vaadin.flow.shared.communication.PushConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentedMessageTest {

    @Test
    public void shortMessageCompleteImmediately() throws IOException {
        FragmentedMessage msg = new FragmentedMessage();
        assertTrue(
                msg.append(new StringReader("Hello".length() + "|" + "Hello")));
        assertEquals("Hello", IOUtils.toString(msg.getReader()));
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

        assertEquals(PushConstants.WEBSOCKET_BUFFER_SIZE, part1.length());
        assertEquals(
                textWithLength.length() - PushConstants.WEBSOCKET_BUFFER_SIZE,
                part2.length());

        StringReader messageReader = new StringReader(part1);
        assertFalse(msg.append(messageReader));
        StringReader messageReader2 = new StringReader(part2);
        assertTrue(msg.append(messageReader2));
        assertEquals(text, IOUtils.toString(msg.getReader()));
    }

    @Test
    public void lengthEqualsLimitHandledCorrectly() throws IOException {
        FragmentedMessage msg = new FragmentedMessage();
        int length = (PushConstants.WEBSOCKET_BUFFER_SIZE
                - String.valueOf(PushConstants.WEBSOCKET_BUFFER_SIZE).length()
                - 1);
        String text = "A".repeat(length);
        String textWithLength = length + "|" + text;

        assertEquals(PushConstants.WEBSOCKET_BUFFER_SIZE,
                textWithLength.length());

        StringReader messageReader = new StringReader(textWithLength);
        assertTrue(msg.append(messageReader));
        assertEquals(text, IOUtils.toString(msg.getReader()));
    }
}
