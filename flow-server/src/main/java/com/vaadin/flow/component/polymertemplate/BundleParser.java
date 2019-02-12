/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component.polymertemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Predicates;
import com.google.javascript.jscomp.NodeUtil;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.parsing.Config;
import com.google.javascript.jscomp.parsing.ParserRunner;
import com.google.javascript.rhino.ErrorReporter;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.SimpleErrorReporter;
import com.google.javascript.rhino.StaticSourceFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public final class BundleParser {

    private static Config config = ParserRunner
            .createConfig(Config.LanguageMode.ECMASCRIPT6, null,
                    Config.StrictMode.STRICT);

    private BundleParser() {
    }

    public static JsonObject getStatisticsJson(String fileName,
            String statistics) {
        return getObjectWithId(fileName, Json.parse(statistics));
    }

    public static Element parseTemplateElement(String fileName,
            JsonObject statisticsJson) {

        ErrorReporter errorReporter = new SimpleErrorReporter();

        // parse a source file into an ast.
        SourceFile sourceFile = new SourceFile(fileName,
                StaticSourceFile.SourceKind.STRONG);

        ParserRunner.ParseResult parseResult = ParserRunner
                .parse(sourceFile, statisticsJson.getString("source"), config,
                        errorReporter);

        // run the visitor on the ast to extract the needed values.
        DependencyVisitor visitor = new DependencyVisitor();
        NodeUtil.visitPreOrder(parseResult.ast, visitor,
                Predicates.alwaysTrue());

        Document templateDocument = Jsoup
                .parse(visitor.getterContent.get("template"));

        Element template = templateDocument.createElement("template");

        templateDocument.body().getAllElements().stream()
                .filter(node -> !node.equals(templateDocument.body()))
                .forEach(template::appendChild);

        return template;
    }

    private static JsonObject getObjectWithId(String fileName,
            JsonObject parse) {
        JsonObject like = null;
        JsonArray chunks = parse.getArray("chunks");
        for (int i = 0; i < chunks.length(); i++) {
            JsonObject obj = chunks.getObject(i);
            JsonArray modules = obj.getArray("modules");
            for (int j = 0; j < modules.length(); j++) {
                JsonObject object = modules.getObject(j);
                if (object.hasKey("id") && (
                        object.getString("id").equals(fileName) || object
                                .getString("id").equals("." + fileName))) {
                    like = object;
                    break;
                }
            }
            if (like != null)
                break;
        }
        return like;
    }

    private static class DependencyVisitor implements NodeUtil.Visitor {

        public List<String> imports = new ArrayList<>();
        public List<String> getters = new ArrayList<>();
        public Map<String, String> getterContent = new HashMap<>();

        @Override
        public void visit(Node node) {
            switch (node.getToken()) {
            case IMPORT:
                addImport(node);
                break;
            case GETTER_DEF:
                addGetter(node);
                break;
            }
        }

        private void addGetter(Node node) {
            getters.add(node.getString());
            if (node.getString().equals("template")) {
                String content = getTextNode(node).getRawString();
                getterContent.put(node.getString(), content);
            }
        }

        private void addImport(Node node) {
            if (node.hasChildren()) {
                Node child = getTextNode(node);
                imports.add(child.getString());
            }
        }

        private Node getTextNode(Node node) {
            Node child = node.getFirstChild();
            while (child.getFirstChild() != null || child.getNext() != null) {
                if (child.getNext() == null) {
                    child = child.getFirstChild();
                } else {
                    child = child.getNext();
                }
            }
            return child;
        }
    }

}
