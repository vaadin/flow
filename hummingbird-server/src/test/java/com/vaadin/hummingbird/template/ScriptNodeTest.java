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
package com.vaadin.hummingbird.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.template.parser.TemplateParser;

public class ScriptNodeTest {

    private TemplateNode parse(String html) {
        return TemplateParser.parse(html);
    }

    @Test
    public void inlineScript() {
        TemplateNode templateNode = parse("<script>\n" //
                + "window.alert('hello');\n" //
                + "window.alert('world');\n" //
                + "</script>" //
        ); //
        Assert.assertEquals(ElementTemplateNode.class, templateNode.getClass());
        ElementTemplateNode elementTemplate = (ElementTemplateNode) templateNode;
        Assert.assertEquals("script", elementTemplate.getTag());

        TextTemplateNode textNode = ((TextTemplateNode) elementTemplate
                .getChild(0));
        String nodeContents = (String) textNode.getTextBinding()
                .getValue(new StateNode());
        Assert.assertEquals(
                "\n" + "window.alert('hello');\n" + "window.alert('world');\n",
                nodeContents);
    }
}
