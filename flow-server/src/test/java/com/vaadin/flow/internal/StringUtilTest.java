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
package com.vaadin.flow.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StringUtilTest {

    @Test
    public void commentRemoval_handlesCommentsCorrectly() {
        String singleLineBlock = StringUtil
                .removeComments("return html'/* single line block comment*/';");

        Assertions.assertEquals("return html'';", singleLineBlock);

        String blockComment = StringUtil
                .removeComments("return html'/* block with new lines\n"
                        + "* still in my/their block */';");
        Assertions.assertEquals("return html'';", blockComment);

        String newLineSingleBlock = StringUtil
                .removeComments("return html'/* not here \n*/';");
        Assertions.assertEquals("return html'';", newLineSingleBlock);

        String noComments = "<vaadin-text-field label=\"Nats Url(s)\" placeholder=\"nats://server:port\" id=\"natsUrlTxt\" style=\"width:100%\"></vaadin-text-field>`";
        Assertions.assertEquals(noComments,
                StringUtil.removeComments(noComments));

        String lineComment = StringUtil
                .removeComments("return html'// this line comment\n';");
        Assertions.assertEquals("return html'\n';", lineComment);

        String mixedComments = StringUtil.removeComments(
                "return html'/* not here \n*/\nCode;// neither this\n"
                        + "/* this should // be fine\n* to remove / */';");
        Assertions.assertEquals("return html'\nCode;\n';", mixedComments);
    }

    @Test
    public void commentRemoval_emojiInString_removalDoesnotThrowResultIsTheSame() {
        String initialTemplate = "import { html } from '@polymer/polymer/lib/utils/html-tag.js';\n"
                + "class EmployeeForm extends PolymerElement {\n"
                + "  static get template() {\n" + "    return html`\n"
                + "   <div style=\"width: 100%; height: 100%;\" class=\"scroll-div\"> \n"
                + "    <iron-pages selected=\"[[page]]\"> \n" + "     <page>\n"
                + "🚧 Training: Coming soon 🚧\n" + "</page> \n"
                + "    </iron-pages> \n" + "   </div> \n" + "`;\n" + "  }\n"
                + "\n";
        String template = StringUtil.removeComments(initialTemplate);
        Assertions.assertEquals(initialTemplate, template);
    }

    @Test
    public void removeComments_commentsWithAsterisksInside_commentIsRemoved() {
        String result = StringUtil.removeComments("/* comment **/ ;");
        Assertions.assertEquals(" ;", result);
    }

    @Test
    public void removeJsComments_handlesApostropheAsInString() {
        String httpImport = "import 'http://localhost:56445/files/transformed/@vaadin/vaadin-text-field/vaadin-text-field.js';";

        Assertions.assertEquals(httpImport,
                StringUtil.removeComments(httpImport, true),
                "Nothing shoiuld be removed for import");

        String result = StringUtil.removeComments("/* comment **/ ;", true);
        Assertions.assertEquals(" ;", result);

        String singleLineBlock = StringUtil.removeComments(
                "return html`/* single line block comment*/`;", true);

        Assertions.assertEquals("return html``;", singleLineBlock);

        String blockComment = StringUtil
                .removeComments("return html`/* block with new lines\n"
                        + "* still in my/their block */`;", true);
        Assertions.assertEquals("return html``;", blockComment);

        String newLineSingleBlock = StringUtil
                .removeComments("return html`/* not here \n*/`;", true);
        Assertions.assertEquals("return html``;", newLineSingleBlock);

        String noComments = "<vaadin-text-field label=\"Nats Url(s)\" placeholder=\"nats://server:port\" id=\"natsUrlTxt\" style=\"width:100%\"></vaadin-text-field>`";
        Assertions.assertEquals(noComments,
                StringUtil.removeComments(noComments, true));

        String lineComment = StringUtil
                .removeComments("return html`// this line comment\n`;", true);
        Assertions.assertEquals("return html`\n`;", lineComment);

        String mixedComments = StringUtil.removeComments(
                "return html`/* not here \n*/\nCode;// neither this\n"
                        + "/* this should // be fine\n* to remove / */`;",
                true);
        Assertions.assertEquals("return html`\nCode;\n`;", mixedComments);
    }

    @Test
    public void stripSuffix() {
        Assertions.assertEquals("foo", StringUtil.stripSuffix("foo", "bar"));
        Assertions.assertEquals("foo", StringUtil.stripSuffix("foobar", "bar"));
        Assertions.assertEquals("foobar",
                StringUtil.stripSuffix("foobarbar", "bar"));
        Assertions.assertEquals("", StringUtil.stripSuffix("", "bar"));
    }
}
