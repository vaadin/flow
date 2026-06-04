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

import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HTMLTest {

    @Test
    void attachedToElement() {
        // This will throw an assertion error if the element is not attached to
        // the component
        new Html("<b>Hello</b>").getParent();
    }

    @Test
    void nullHtml() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html((String) null);
        });
    }

    @Test
    void nullStream() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html((InputStream) null);
        });
    }

    @Test
    void emptyHtml() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html("");
        });
    }

    @Test
    void twoRoots() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html("<b></b><div></div>");
        });
    }

    @Test
    void text() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Html("hello");
        });
    }

    @Test
    void simpleHtml() {
        Html html = new Html("<span>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals("hello", html.getInnerHtml());
    }

    @Test
    void setHtmlContent() {
        Html html = new Html("<span>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals("hello", html.getInnerHtml());
        html.setHtmlContent("<span>world</span>");
        assertEquals("world", html.getInnerHtml());
    }

    @Test
    void setHtmlContent_tagMismatch() {
        Html html = new Html("<span>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals("hello", html.getInnerHtml());
        assertThrows(IllegalStateException.class,
                () -> html.setHtmlContent("<div>world</div>"));
    }

    @Test
    void rootAttributes() {
        Html html = new Html("<span foo='bar'>hello</span>");
        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals(1, html.getElement().getAttributeNames().count());
        assertEquals("bar", html.getElement().getAttribute("foo"));
        assertEquals("hello", html.getInnerHtml());
    }

    @Test
    void rootSpecialAttributes() {
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
    void fromStream() {
        new Html(new ByteArrayInputStream(
                "<div><span>contents</span></div>".getBytes()));
    }

    @Test
    void brokenHtml() {
        Html html = new Html("<b></div>");
        assertEquals("b", html.getElement().getTag());
        assertEquals("", html.getInnerHtml());
    }

    @Test
    void extraWhitespace() {
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
    void emptyAttribute_elementIsCreatedAndHasAttribute() {
        Html html = new Html("<audio controls></audio>");

        assertEquals("", html.getElement().getAttribute("controls"));

        assertEquals("<audio controls></audio>",
                html.getElement().getOuterHTML());
    }

    @Test
    void styleElementAsString_elementIsUsed() {
        Html html = new Html("<style></style>");
        assertEquals("style", html.getElement().getTag());
    }

    @Test

    void styleElementAsStream_elementIsUsed() {
        Html html = new Html(new ByteArrayInputStream(
                "<style></style>".getBytes(StandardCharsets.UTF_8)));
        assertEquals("style", html.getElement().getTag());
    }

    @Test
    void stringWithSafelist_disallowedContentRemoved() {
        Safelist safelist = new Safelist().addTags("span", "b")
                .addAttributes("span", "title");
        Html html = new Html("<span title='ok' onclick='evil()'>hi<b>there</b>"
                + "<script>steal()</script></span>", safelist);

        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertEquals("ok", html.getElement().getAttribute("title"));
        assertNull(html.getElement().getAttribute("onclick"));
        assertEquals("hi<b>there</b>", html.getInnerHtml());
    }

    @Test
    void stringWithSafelist_rootStrippedBySafelist_throws() {
        // none() permits no tags, so the only root element is removed and the
        // remaining text leaves no top-level element
        assertThrows(IllegalArgumentException.class,
                () -> new Html("<span>hello</span>", Safelist.none()));
    }

    @Test
    void stringWithSafelist_nullSafelist_throws() {
        assertThrows(NullPointerException.class,
                () -> new Html("<span>hi</span>", (Safelist) null));
    }

    @Test
    void streamWithSafelist_disallowedContentRemoved() {
        Safelist safelist = new Safelist().addTags("div");
        Html html = new Html(new ByteArrayInputStream(
                "<div onclick='evil()'><script>x()</script>text</div>"
                        .getBytes(StandardCharsets.UTF_8)),
                safelist);

        assertEquals("div", html.getElement().getTag());
        assertNull(html.getElement().getAttribute("onclick"));
        assertEquals("text", html.getInnerHtml());
    }

    @Test
    void streamWithSafelist_nullSafelist_throws() {
        assertThrows(NullPointerException.class,
                () -> new Html(
                        new ByteArrayInputStream(
                                "<div></div>".getBytes(StandardCharsets.UTF_8)),
                        (Safelist) null));
    }

    @Test
    void setHtmlContentWithSafelist_disallowedContentRemoved() {
        Safelist safelist = new Safelist().addTags("span");
        Html html = new Html("<span>initial</span>");

        html.setHtmlContent(
                "<span onclick='evil()'>clean<script>x()</script></span>",
                safelist);

        assertEquals(Tag.SPAN, html.getElement().getTag());
        assertNull(html.getElement().getAttribute("onclick"));
        assertEquals("clean", html.getInnerHtml());
    }

    @Test
    void setHtmlContentWithSafelist_nullSafelist_throws() {
        Html html = new Html("<span>initial</span>");
        assertThrows(NullPointerException.class,
                () -> html.setHtmlContent("<span>x</span>", (Safelist) null));
    }

}
