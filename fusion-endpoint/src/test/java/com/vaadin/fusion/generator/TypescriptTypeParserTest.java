package com.vaadin.fusion.generator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TypescriptTypeParserTest {
    @Test
    public void should_AllowNodeReplacing() {
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(
                "Record<ReadonlyArray<string>, Record<string, ReadonlyArray<string>>>")
                .traverse().visit((current, parent) -> {
                    if ("Record".equals(current.getName())
                            && parent.map(p -> !"Readonly".equals(p.getName()))
                                    .orElse(true)) {
                        TypescriptTypeParser.Node w = new TypescriptTypeParser.Node(
                                "Readonly");
                        w.addNested(current);

                        return w;
                    }

                    return current;
                }).finish();

        assertEquals(
                "Readonly<Record<ReadonlyArray<string>, Readonly<Record<string, ReadonlyArray<string>>>>>",
                node.toString());
    }

    @Test
    public void should_AllowTypeRenaming() {
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(
                "Map<ReadonlyArray<string>, Map<string, ReadonlyArray<string>>>")
                .traverse().visit((current, _parent) -> {
                    if ("Map".equals(current.getName())) {
                        current.setName("Record");
                        return current;
                    }

                    return current;
                }).finish();

        assertEquals(
                "Record<ReadonlyArray<string>, Record<string, ReadonlyArray<string>>>",
                node.toString());
    }

    @Test
    public void should_AllowsTypeNullability() {
        String type = "Readonly<Record<string, Readonly<Record<string, ReadonlyArray<MyEntity | undefined> | undefined>> | undefined>>";
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(type);
        assertEquals(type, node.toString());
    }

    @Test
    public void should_ParseComplexType() {
        String type = "Map<ReadonlyArray<string>, Map<string, ReadonlyArray<string>>>";
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(type);

        assertEquals(type, node.toString());
    }

    @Test
    public void should_ParseSimpleType() {
        String type = "string";
        TypescriptTypeParser.Node node = TypescriptTypeParser.parse(type);

        assertEquals(type, node.toString());
    }
}
