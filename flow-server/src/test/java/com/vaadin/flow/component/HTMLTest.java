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
package com.vaadin.flow.component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HTMLTest {

    @Test
    public void attachedToElement() {
        // This will throw an assertion error if the element is not attached to
        // the component
        new Html("<b>Hello</b>").getParent();
    }

    @Test
    public void nullHtml() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html((String) null);
        });
    }

    @Test
    public void nullStream() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html((InputStream) null);
        });
    }

    @Test
    public void emptyHtml() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html("");
        });
    }

    @Test
    public void twoRoots() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html("<b></b><div></div>");
        });
    }

    @Test
    public void text() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html("hello");
        });
    }

    @Test
    public void simpleHtml() {
        Html html = new Html("<span>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals("hello", html.getInnerHtml());
    }

    @Test
    public void setHtmlContent() {
        Html html = new Html("<span>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals("hello", html.getInnerHtml());
        html.setHtmlContent("<span>world</span>");
        assertEquals("world", html.getInnerHtml());
    }

    @Test
    public void setHtmlContent_tagMismatch() {
        Html html = new Html("<span>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals("hello", html.getInnerHtml());
        assertThrows(IllegalStateException.class,
                () -> html.setHtmlContent("<div>world</div>"));
    }

    @Test
    public void rootAttributes() {
        Html html = new Html("<span foo='bar'>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals(1, html.getElement().getAttributeNames().count());
        assertEquals("bar", html.getElement().getAttribute("foo"));
        assertEquals("hello", html.getInnerHtml());
    }

    @Test
    public void rootSpecialAttributes() {
        Html html = new Html(
                "<span class='foo' style='color: red'>hello</span>");
        Element element = html.getElement();
        assertEquals(Tag.SPAN, element.getTag());

        assertEquals(2, element.getAttributeNames().count());
        assertEquals("foo", element.getAttribute("class"));
        assertEquals("color:red", element.getAttribute("style"));
        assertEquals("hello", html.getInnerHtml());
    }

    @Test
    public void fromStream() {
        new Html(new ByteArrayInputStream(
                "<div><span>contents</span></div>".getBytes()));
    }

    @Test
    public void brokenHtml() {
        Html html = new Html("<b></div>");
        assertEquals("b", html.getElement().getTag());
        assertEquals("", html.getInnerHtml());
    }

    @Test
    public void extraWhitespace() {
        String input = "   <span>    " //
                + "    <div>" //
                + "       <b>Hello!</b>" //
                + "    </div>" //
                + "</span>" + "  " //
                + "" //
                + "";
        Html html = new Html(input);
        assertEquals(Tag.SPAN, html.getElement().getTag());
        String expectedInnerHtml = input.replaceAll("^[ ]*<span>", "")
                .replaceAll("</span>[ ]*$", "");
        assertEquals(expectedInnerHtml, html.getInnerHtml());
    }

    @Test
    public void emptyAttribute_elementIsCreatedAndHasAttribute() {
        Html html = new Html("<audio controls></audio>");

        assertEquals("", html.getElement().getAttribute("controls"));

        assertEquals("<audio controls></audio>",
                html.getElement().getOuterHTML());
    }

    @Test
    public void styleElementAsString_elementIsUsed() {
        Html html = new Html("<style></style>");
        assertEquals("style", html.getElement().getTag());
    }

    @Test

    public void styleElementAsStream_elementIsUsed() {
        Html html = new Html(new ByteArrayInputStream(
                "<style></style>".getBytes(StandardCharsets.UTF_8)));
        assertEquals("style", html.getElement().getTag());
    }

}
