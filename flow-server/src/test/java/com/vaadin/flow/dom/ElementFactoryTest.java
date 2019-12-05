/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public class ElementFactoryTest {

    private Map<String, String> methodToTag = new HashMap<>();

    {
        methodToTag.put("anchor", "a");
        methodToTag.put("horizontalrule", "hr");
        methodToTag.put("preformatted", "pre");
        methodToTag.put("paragraph", "p");
        methodToTag.put("emphasis", "em");
        methodToTag.put("listitem", "li");
        methodToTag.put("unorderedlist", "ul");
        for (int i = 1; i <= 6; i++) {
            methodToTag.put("heading" + i, "h" + i);
        }
    }

    @Test
    public void automatedTest() throws Exception {
        for (Method method : ElementFactory.class.getMethods()) {
            testMethod(method);
        }
    }

    @Test
    public void createAnchor() {
        String href = "hrefhref";
        String textContent = "textContent";

        assertElement("<a href='" + href + "'></a>",
                ElementFactory.createAnchor(href));
        assertElement("<a href='" + href + "'>textContent</a>",
                ElementFactory.createAnchor(href, textContent));
        assertElement("<a href='" + href + "' router-link=''>textContent</a>",
                ElementFactory.createRouterLink(href, textContent));
    }

    @Test
    public void createTextInput() {
        String type = "typetype";

        assertElement("<input type='" + type + "'></input>",
                ElementFactory.createInput(type));
    }

    private void assertElement(String expectedOuterHtml, Element createAnchor) {
        String actualHtml = getOuterHtml(createAnchor);
        Assert.assertEquals(expectedOuterHtml, actualHtml);
    }

    private String getOuterHtml(Element e) {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(e.getTag());
        String attrs = e.getAttributeNames().sorted()
                .map(name -> name + "='" + e.getAttribute(name) + "'")
                .collect(Collectors.joining(" "));
        if (!attrs.isEmpty()) {
            sb.append(" ").append(attrs);
        }
        sb.append(">");
        sb.append(e.getTextRecursively());
        sb.append("</");
        sb.append(e.getTag());
        sb.append(">");

        return sb.toString();
    }

    private boolean isSimpleCreateMethod(Method method) {
        if (!method.getName().startsWith("create")) {
            return false;
        }

        return method.getParameterTypes().length == 0;
    }

    private void testMethod(Method method) throws Exception {
        if (isTestedSeparately(method)) {
            return;
        } else if (isSimpleCreateMethod(method)) {
            Element element = (Element) method.invoke(null);
            String expectedTag = tagNameFromMethod(method);

            assertElement("<" + expectedTag + "></" + expectedTag + ">",
                    element);
        } else if (isTextContentMethod(method)) {
            Element element = (Element) method.invoke(null, "textContent");
            String expectedTag = tagNameFromMethod(method);

            assertElement(
                    "<" + expectedTag + ">textContent</" + expectedTag + ">",
                    element);
        } else {
            Assert.fail("Untested method: " + method.getName() + "("
                    + Stream.of(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(","))
                    + ")");
        }
    }

    private boolean isTestedSeparately(Method method) {
        if (method.getName().equals("createAnchor")
                && method.getParameterTypes().length > 0) {
            return true;
        }
        if (method.getName().equals("createRouterLink")
                && method.getParameterTypes().length > 0) {
            return true;
        }
        if (method.getName().equals("createInput")
                && method.getParameterTypes().length > 0) {
            return true;
        }

        return false;
    }

    private boolean isTextContentMethod(Method method) {
        // Quite broad assumption - exceptions are handled as separate tests
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 1 && parameterTypes[0] == String.class;
    }

    private String tagNameFromMethod(Method method) {
        String tagFromMethod = method.getName().replace("create", "")
                .toLowerCase(Locale.ENGLISH);
        if (methodToTag.containsKey(tagFromMethod)) {
            return methodToTag.get(tagFromMethod);
        } else {
            return tagFromMethod;
        }
    }
}
