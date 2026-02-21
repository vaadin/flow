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
package com.vaadin.flow.component.page;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ClipboardEventTest {

    @Test
    public void textEvent_hasCorrectValues() {
        ClipboardEvent event = new ClipboardEvent("paste", "hello", null,
                List.of());

        Assert.assertEquals("paste", event.getType());
        Assert.assertEquals("hello", event.getText());
        Assert.assertNull(event.getHtml());
        Assert.assertTrue(event.hasText());
        Assert.assertFalse(event.hasHtml());
        Assert.assertFalse(event.hasFiles());
        Assert.assertTrue(event.getFiles().isEmpty());
    }

    @Test
    public void htmlEvent_hasCorrectValues() {
        ClipboardEvent event = new ClipboardEvent("paste", "plain",
                "<b>bold</b>", List.of());

        Assert.assertTrue(event.hasText());
        Assert.assertTrue(event.hasHtml());
        Assert.assertEquals("<b>bold</b>", event.getHtml());
    }

    @Test
    public void emptyTextAndHtml_hasTextReturnsFalse() {
        ClipboardEvent event = new ClipboardEvent("paste", "", "", List.of());

        Assert.assertFalse(event.hasText());
        Assert.assertFalse(event.hasHtml());
    }

    @Test
    public void nullTextAndHtml_hasTextReturnsFalse() {
        ClipboardEvent event = new ClipboardEvent("paste", null, null,
                List.of());

        Assert.assertFalse(event.hasText());
        Assert.assertFalse(event.hasHtml());
    }

    @Test
    public void eventWithFiles_hasFilesReturnsTrue() {
        ClipboardFile file = new ClipboardFile("test.png", "image/png", 100,
                new byte[100]);
        ClipboardEvent event = new ClipboardEvent("paste", null, null,
                List.of(file));

        Assert.assertTrue(event.hasFiles());
        Assert.assertEquals(1, event.getFiles().size());
        Assert.assertEquals("test.png", event.getFiles().get(0).getName());
    }

    @Test
    public void filesListIsUnmodifiable() {
        ClipboardEvent event = new ClipboardEvent("paste", null, null, List
                .of(new ClipboardFile("f.txt", "text/plain", 5, new byte[5])));

        Assert.assertThrows(UnsupportedOperationException.class, () -> event
                .getFiles()
                .add(new ClipboardFile("x.txt", "text/plain", 1, new byte[1])));
    }

    @Test
    public void nullFilesList_becomesEmptyList() {
        ClipboardEvent event = new ClipboardEvent("paste", "text", null, null);

        Assert.assertNotNull(event.getFiles());
        Assert.assertTrue(event.getFiles().isEmpty());
        Assert.assertFalse(event.hasFiles());
    }

    @Test
    public void copyEvent_hasCorrectType() {
        ClipboardEvent event = new ClipboardEvent("copy", "copied text", null,
                List.of());
        Assert.assertEquals("copy", event.getType());
    }

    @Test
    public void cutEvent_hasCorrectType() {
        ClipboardEvent event = new ClipboardEvent("cut", "cut text", null,
                List.of());
        Assert.assertEquals("cut", event.getType());
    }

    @Test
    public void clipboardFile_hasCorrectValues() {
        byte[] data = { 1, 2, 3, 4, 5 };
        ClipboardFile file = new ClipboardFile("photo.jpg", "image/jpeg", 5,
                data);

        Assert.assertEquals("photo.jpg", file.getName());
        Assert.assertEquals("image/jpeg", file.getMimeType());
        Assert.assertEquals(5, file.getSize());
        Assert.assertArrayEquals(data, file.getData());
    }
}
