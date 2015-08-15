package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildTodoMvcStringPerformance {
    public static void main(String[] args) throws InterruptedException {
        int modelSize = 1000;
        StateNode todoModel = buildTodoModel(modelSize);
        ElementTemplate template = buildTemplate();

        // Warm up
        long warmupStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - warmupStart < 10000) {
            buildHtml(template, todoModel);
        }
        System.gc();
        Thread.sleep(1000);

        long length = 0;
        int iterations = 100;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String html = buildHtml(template, todoModel);
            length += html.length();
        }
        long end = System.nanoTime();

        System.out.println("Template based with " + modelSize + " items");
        System.out.println("Bytes produced: " + length);
        System.out.println("Time per iteration: "
                + (end - start) / ((double) iterations * 1000000));

    }

    private static String buildHtml(ElementTemplate template,
            StateNode todoModel) {
        return Element.getElement(template, todoModel).toString();
    }

    private static ElementTemplate buildTemplate() {
        List<AttributeBinding> el = Collections.emptyList();
        Map<String, String> em = Collections.emptyMap();

        BoundElementTemplate headerH1 = new BoundElementTemplate("h1", el, em,
                Collections.singletonList(new StaticTextTemplate("todos")));

        BoundElementTemplate headerInput = new BoundElementTemplate("input", el,
                buildMap("id", "new-todo", "class", "new-todo", "placeholder",
                        "What needs to be done?", "autofocus", ""));

        BoundElementTemplate header = new BoundElementTemplate("header", el,
                buildMap("class", "header"),
                Arrays.asList(headerH1, headerInput));

        BoundElementTemplate toggleAll = new BoundElementTemplate("input",
                Collections.singletonList(new AttributeBinding("checked") {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.containsKey("allCompleted")) {
                            return "";
                        } else {
                            return null;
                        }
                    }
                }), buildMap("id", "toggle-all", "class", "toggle-all", "type",
                        "checkbox"));

        BoundElementTemplate mainLabel = new BoundElementTemplate("label", el,
                buildMap("for", "toggle-all"), Collections.singletonList(
                        new StaticTextTemplate("Mark all as completed")));

        BoundElementTemplate toggle = new BoundElementTemplate("input",
                Collections.singletonList(new AttributeBinding("checked") {
                    @Override
                    public String getValue(StateNode node) {
                        if (Boolean.TRUE.equals(node.get("completed"))) {
                            return "checked";
                        } else {
                            return null;
                        }
                    }
                }), buildMap("class", "toggle", "type", "checkbox"));

        BoundElementTemplate todoLabel = new BoundElementTemplate("label", el,
                em,
                Collections.singletonList(new DynamicTextTemplate("title")));

        BoundElementTemplate todoButton = new BoundElementTemplate("button", el,
                buildMap("class", "destroy"));

        BoundElementTemplate view = new BoundElementTemplate("div",
                Collections.singletonList(new AttributeBinding("class") {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.get("id")
                                .equals(node.getParent().get("editId"))) {
                            return "hidden";
                        } else {
                            return null;
                        }
                    }
                }), em, Arrays.asList(toggle, todoLabel, todoButton));

        BoundElementTemplate edit = new BoundElementTemplate("input",
                Arrays.asList(new ModelAttributeBinding("value", "title"),
                        new AttributeBinding("class") {
                            @Override
                            public String getValue(StateNode node) {
                                if (node.get("id").equals(
                                        node.getParent().get("editId"))) {
                                    return "edit";
                                } else {
                                    return "edit hidden";
                                }
                            }
                        }),
                em);

        BoundElementTemplate todoLi = new ForElementTemplate("li",
                Collections.singletonList(new AttributeBinding("class") {
                    @Override
                    public String getValue(StateNode node) {
                        ArrayList<String> classes = new ArrayList<>();
                        if (Boolean.TRUE.equals(node.get("completed"))) {
                            classes.add("completed");
                        }
                        if (node.get("id")
                                .equals(node.getParent().get("editId"))) {
                            classes.add("editing");
                        }
                        if (classes.isEmpty()) {
                            return null;
                        } else {
                            return classes.stream()
                                    .collect(Collectors.joining(" "));
                        }
                    }
                }), em, "todos", Arrays.asList(view, edit));

        BoundElementTemplate todoList = new BoundElementTemplate("ul", el,
                buildMap("class", "todo-list"),
                Collections.singletonList(todoLi));

        BoundElementTemplate todoCountStrong = new BoundElementTemplate(
                "strong", el, em,
                Collections.singletonList(new DynamicTextTemplate(n -> {
                    List<Object> list = n.getMultiValued("todos");
                    long remainingCount = list
                            .stream().filter(o -> ((StateNode) o)
                                    .get("completed").equals(Boolean.FALSE))
                            .count();
                    return Long.toString(remainingCount);
                })));

        BoundElementTemplate todoCount = new BoundElementTemplate("span", el,
                buildMap("class", "todo-count"), Arrays.asList(todoCountStrong,
                        new StaticTextTemplate(" items left")));

        BoundElementTemplate clearCompleted = new BoundElementTemplate("button",
                Collections.singletonList(new AttributeBinding("class") {
                    @Override
                    public String getValue(StateNode n) {
                        List<Object> list = n.getMultiValued("todos");
                        boolean hasCompleted = list.stream()
                                .anyMatch(o -> ((StateNode) o).get("completed")
                                        .equals(Boolean.TRUE));
                        if (!hasCompleted) {
                            return "hidden";
                        } else {
                            return null;
                        }
                    }
                }), buildMap("id", "clear-completed"));

        BoundElementTemplate footer = new BoundElementTemplate("footer",
                Collections.singletonList(new AttributeBinding("class") {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.getMultiValued("todos").isEmpty()) {
                            return "hidden";
                        } else {
                            return null;
                        }
                    }
                }), em, Arrays.asList(todoCount, clearCompleted));

        BoundElementTemplate main = new BoundElementTemplate("section",
                Collections.singletonList(new AttributeBinding("class") {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.getMultiValued("todos").isEmpty()) {
                            return "main hidden";
                        } else {
                            return "main";
                        }
                    }
                }), em, Arrays.asList(toggleAll, mainLabel, todoList, footer));

        BoundElementTemplate todoapp = new BoundElementTemplate("section", el,
                buildMap("class", "todoapp"), Arrays.asList(header, main));

        BoundElementTemplate templateByLink = new BoundElementTemplate("a", el,
                buildMap("href", "http://sindresorhus.com"),
                Collections.singletonList(
                        new StaticTextTemplate("Sindre Sorhus")));
        BoundElementTemplate templateBy = new BoundElementTemplate("p", el, em,
                Arrays.asList(new StaticTextTemplate("Template by "),
                        templateByLink));

        BoundElementTemplate createdByLink = new BoundElementTemplate("a", el,
                buildMap("href", "http://vaadin.com"),
                Collections.singletonList(
                        new StaticTextTemplate("A humming bird")));
        BoundElementTemplate createdBy = new BoundElementTemplate("p", el, em,
                Arrays.asList(new StaticTextTemplate("Created by "),
                        createdByLink));

        BoundElementTemplate partOfLink = new BoundElementTemplate("a", el,
                buildMap("href", "http://todomvc.com"),
                Collections.singletonList(new StaticTextTemplate("TodoMVC")));
        BoundElementTemplate partOf = new BoundElementTemplate("p", el, em,
                Arrays.asList(new StaticTextTemplate("Part of "), partOfLink));

        BoundElementTemplate info = new BoundElementTemplate("footer", el,
                buildMap("class", "info"),
                Arrays.asList(templateBy, createdBy, partOf));

        BoundElementTemplate body = new BoundElementTemplate("body", el, em,
                Arrays.asList(todoapp, info));
        return body;
    }

    private static Map<String, String> buildMap(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static StateNode buildTodoModel(int count) {
        StateNode node = StateNode.create();
        List<Object> list = node.getMultiValued("todos");

        for (int i = 0; i < count; i++) {
            StateNode todo = StateNode.create();
            todo.put("id", Integer.valueOf(i));
            todo.put("title", "Todo " + i);
            todo.put("completed", Boolean.FALSE);

            list.add(todo);
        }
        return node;
    }
}
