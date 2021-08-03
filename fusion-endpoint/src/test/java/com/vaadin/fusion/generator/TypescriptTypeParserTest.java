package com.vaadin.fusion.generator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TypescriptTypeParserTest {
    @Test
    public void should_AllowNodeReplacing() {
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(
                "Record<ReadonlyArray<string>, Record<string, ReadonlyArray<string>>>");

        TypescriptTypeParser.Node wrapper = new TypescriptTypeParser.Node(
                "Readonly");
        wrapper.addNested(node);

        node.visit((current, parent) -> {
            if ("Record".equals(current.getName())
                    && !"Readonly".equals(parent.getName())) {
                TypescriptTypeParser.Node w = new TypescriptTypeParser.Node(
                        "Readonly");
                w.addNested(current);

                return w;
            }

            return current;
        });

        assertEquals(wrapper.toString(),
                "Readonly<Record<ReadonlyArray<string>, Readonly<Record<string, ReadonlyArray<string>>>>>");
    }

    @Test
    public void should_AllowTypeRenaming() {
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(
                "Map<ReadonlyArray<string>, Map<string, ReadonlyArray<string>>>");
        node.setName("Record");

        node.visit((current, _parent) -> {
            if ("Map".equals(current.getName())) {
                current.setName("Record");
                return current;
            }

            return current;
        });

        assertEquals(node.toString(),
                "Record<ReadonlyArray<string>, Record<string, ReadonlyArray<string>>>");
    }

    @Test
    public void should_ParseComplexType() {
        String type = "Map<ReadonlyArray<string>, Map<string, ReadonlyArray<string>>>";
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(type);

        assertEquals(node.toString(), type);
    }

    @Test
    public void should_ParseSimpleType() {
        String type = "string";
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(type);

        assertEquals(node.toString(), type);
    }
}
