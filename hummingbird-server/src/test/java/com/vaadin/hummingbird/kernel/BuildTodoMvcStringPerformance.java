package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.List;
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
        BoundTemplateBuilder headerH1 = TemplateBuilder.withTag("h1")
                .addChild(TemplateBuilder.staticText("todos"));

        BoundTemplateBuilder headerInput = TemplateBuilder.withTag("input")
                .setAttribute("id", "new-todo")
                .setAttribute("class", "new-todo")
                .setAttribute("placeholder", "What needs to be done?")
                .setAttribute("autofocus", "");

        BoundTemplateBuilder header = TemplateBuilder.withTag("header")
                .setAttribute("class", "header").addChild(headerH1)
                .addChild(headerInput);

        BoundTemplateBuilder toggleAll = TemplateBuilder.withTag("input")
                .bindAttribute("checked", new Binding() {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.containsKey("allCompleted")) {
                            return "";
                        } else {
                            return null;
                        }
                    }
                }).setAttribute("id", "toggle-all")
                .setAttribute("class", "toggle-all")
                .setAttribute("type", "checkbox");

        BoundTemplateBuilder mainLabel = TemplateBuilder.withTag("label")
                .setAttribute("for", "toggle-all")
                .addChild(TemplateBuilder.staticText("Mark all as completed"));

        BoundTemplateBuilder toggle = TemplateBuilder.withTag("input")
                .bindAttribute("checked", new Binding() {
                    @Override
                    public String getValue(StateNode node) {
                        if (Boolean.TRUE.equals(node.get("completed"))) {
                            return "checked";
                        } else {
                            return null;
                        }
                    }
                }).setAttribute("class", "toggle")
                .setAttribute("type", "checkbox");

        BoundTemplateBuilder todoLabel = TemplateBuilder.withTag("label")
                .addChild(TemplateBuilder.dynamicText("title"));

        BoundTemplateBuilder todoButton = TemplateBuilder.withTag("button")
                .setAttribute("class", "destory");

        BoundTemplateBuilder view = TemplateBuilder.withTag("div")
                .bindAttribute("class", new Binding() {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.get("id")
                                .equals(node.getParent().get("editId"))) {
                            return "hidden";
                        } else {
                            return null;
                        }
                    }
                }).addChild(toggle).addChild(todoLabel).addChild(todoButton);

        BoundTemplateBuilder edit = TemplateBuilder.withTag("input")
                .bindAttribute("value", "title")
                .bindAttribute("class", new Binding() {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.get("id")
                                .equals(node.getParent().get("editId"))) {
                            return "edit";
                        } else {
                            return "edit hidden";
                        }
                    }
                });

        BoundTemplateBuilder todoLi = TemplateBuilder.withTag("li")
                .bindAttribute("class", new Binding() {
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
                }).setForDefinition(new ModelPath("todos"), null).addChild(view)
                .addChild(edit);

        BoundTemplateBuilder todoList = TemplateBuilder.withTag("ul")
                .setAttribute("class", "todo-list").addChild(todoLi);

        BoundTemplateBuilder todoCountStrong = TemplateBuilder.withTag("strong")
                .addChild(TemplateBuilder.dynamicText(n -> {
                    List<Object> list = n.getMultiValued("todos");
                    long remainingCount = list
                            .stream().filter(o -> ((StateNode) o)
                                    .get("completed").equals(Boolean.FALSE))
                            .count();
                    return Long.toString(remainingCount);
                }));

        BoundTemplateBuilder todoCount = TemplateBuilder.withTag("span")
                .setAttribute("class", "todo-count").addChild(todoCountStrong)
                .addChild(TemplateBuilder.staticText(" items left"));

        BoundTemplateBuilder clearCompleted = TemplateBuilder.withTag("button")
                .bindAttribute("class", new Binding() {
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
                }).setAttribute("id", "clear-completed");
        BoundTemplateBuilder footer = TemplateBuilder.withTag("footer")
                .bindAttribute("class", new Binding() {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.getMultiValued("todos").isEmpty()) {
                            return "hidden";
                        } else {
                            return null;
                        }
                    }
                }).addChild(todoCount).addChild(clearCompleted);

        BoundTemplateBuilder main = TemplateBuilder.withTag("section")
                .bindAttribute("class", new Binding() {
                    @Override
                    public String getValue(StateNode node) {
                        if (node.getMultiValued("todos").isEmpty()) {
                            return "main hidden";
                        } else {
                            return "main";
                        }
                    }
                }).addChild(toggleAll).addChild(mainLabel).addChild(todoList)
                .addChild(footer);

        BoundTemplateBuilder todoapp = TemplateBuilder.withTag("section")
                .setAttribute("class", "todoapp").addChild(header)
                .addChild(main);

        BoundTemplateBuilder templateByLink = TemplateBuilder.withTag("a")
                .setAttribute("href", "http://sindresorhus.com")
                .addChild(TemplateBuilder.staticText("Sindre Sorhus"));

        BoundTemplateBuilder templateBy = TemplateBuilder.withTag("p")
                .addChild(TemplateBuilder.staticText("Template by "))
                .addChild(templateByLink);

        BoundTemplateBuilder createdByLink = TemplateBuilder.withTag("a")
                .setAttribute("href", "http://vaadin.com")
                .addChild(TemplateBuilder.staticText("A humming bird"));

        BoundTemplateBuilder createdBy = TemplateBuilder.withTag("p")
                .addChild(TemplateBuilder.staticText("Created by "))
                .addChild(createdByLink);

        BoundTemplateBuilder partOfLink = TemplateBuilder.withTag("a")
                .setAttribute("href", "http://todomvc.com")
                .addChild(TemplateBuilder.staticText("TodoMVC"));

        BoundTemplateBuilder partOf = TemplateBuilder.withTag("p")
                .addChild(TemplateBuilder.staticText("Part of "))
                .addChild(partOfLink);

        BoundTemplateBuilder info = TemplateBuilder.withTag("footer")
                .setAttribute("class", "info").addChild(templateBy)
                .addChild(createdBy).addChild(partOf);

        BoundTemplateBuilder body = TemplateBuilder.withTag("body")
                .addChild(todoapp).addChild(info);

        return body.build();
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
