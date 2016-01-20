package com.vaadin.hummingbird.json;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.AbstractElementTemplate;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.parser.TemplateParser;
import com.vaadin.server.communication.TemplateSerializer;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.impl.JsonUtil;

public class TemplateSerializerTest {

    @Before
    public void resetTemplateId()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field f = AbstractElementTemplate.class.getDeclaredField("nextId");
        f.setAccessible(true);
        AtomicInteger nextId = (AtomicInteger) f.get(null);
        nextId.set(1);
    }

    @Test
    public void testTemplateIndexVariable() {
        String serialized = serialize(
                "<ul><li *ng-for='#item of items;#i=index' [value]='i'></li></ul>");
        System.out.println(serialized);

    }

    @Test
    public void testTemplateLastVariable() {
        String serialized = serialize(
                "<ul><li *ng-for='#item of items;#i=last' [value]='i'></li></ul>");
        System.out.println(serialized);

    }

    @Test
    public void testTemplateEvenVariable() {
        String serialized = serialize(
                "<ul><li *ng-for='#item of items;#i=even' [value]='i'></li></ul>");
        System.out.println(serialized);

    }

    @Test
    public void testTemplateOddVariable() {
        String serialized = serialize(
                "<ul><li *ng-for='#item of items;#i=odd' [value]='i'></li></ul>");
        System.out.println(serialized);
    }

    @Test
    public void testNestedTemplateIndexVariable() {
        String tpl = "<table>" //
                + "<tr *ng-for='#row of rows; #rowIndex = index;' [attr.clazz]='\"row-\"+rowIndex'>"
                + "<td *ng-for='#cell of row.columns; #colIndex=index;' [attr.clazz]='\"row-\"+rowIndex+\"-col-\"+colIndex'>{{cell.content}}</td>"
                + "</tr>" //
                + "</table>";
        tpl = tpl.replace("\"", "§");
        String serialized = serialize(tpl);
        serialized = serialized.replace("§", "\"");
        System.out.println(serialized);
    }

    @Test
    public void testMultipleNestedTemplateVariables() {
        String serialized = serialize("<ul>" //
                + "<li *ng-for='#row of rows; #rowIndex = index; #rowLast=last;#rowOdd=odd;#rowEven=even' [class.odd]='rowOdd' [class.even]='rowEven' [class.last]='rowLast'</li>"
                + "</ul>");
        System.out.println(serialized);
    }

    private String serialize(String template) {
        BoundElementTemplate elementTemplate = (BoundElementTemplate) TemplateParser
                .parse(template);
        JsonArray array = Json.createArray();
        serialize(elementTemplate, array);

        String string = JsonUtil.stringify(array, 0);
        return string.replace("\"", "'").replace("{'type':", "\n{'type':")
                + "\n";
    }

    private void serialize(BoundElementTemplate elementTemplate,
            JsonArray array) {
        TemplateSerializer serializer = new TemplateSerializer(null);
        array.set(array.length(),
                serializer.serializeTemplate(elementTemplate));

        if (elementTemplate.getChildTemplates() != null) {
            for (BoundElementTemplate child : elementTemplate
                    .getChildTemplates()) {
                serialize(child, array);
            }
        }
    }
}
