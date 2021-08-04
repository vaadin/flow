package com.vaadin.fusion.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class TypescriptTypeParser {
    static Node parse(String type) throws SyntaxError {
        List<Token> tokens = Token.process(type);
        return Node.process(tokens);
    }

    static class BraceToken extends Token {
        private final boolean closing;

        BraceToken(boolean closing) {
            this.closing = closing;
        }

        boolean isClosing() {
            return closing;
        }
    }

    static class NameToken extends Token {
        private final StringBuilder name = new StringBuilder();

        void append(char ch) {
            name.append(ch);
        }

        String getName() {
            return name.toString();
        }
    }

    static class Node implements Cloneable {
        private String name;
        private List<Node> nested = new ArrayList<>();
        private boolean undefined = false;

        public Node(String name) {
            this.name = name;
        }

        @SuppressWarnings("squid:S134")
        private static Node process(List<Token> tokens) {
            Stack<Node> unclosedNodes = new Stack<>();
            Node currentNode = null;
            boolean waitingForSuffix = false;

            for (final Token token : tokens) {
                if (token instanceof NameToken) {
                    if (currentNode != null && waitingForSuffix) {
                        if (((NameToken) token).getName().equals("undefined")) {
                            currentNode.setUndefined(true);
                            waitingForSuffix = false;
                        } else {
                            throw new SyntaxError(String.format(
                                    "Type union '{} | {}' is not expected",
                                    currentNode.getName(),
                                    ((NameToken) token).getName()));
                        }
                    } else {
                        currentNode = new Node(((NameToken) token).getName());

                        if (!unclosedNodes.empty()) {
                            unclosedNodes.peek().addNested(currentNode);
                        }
                    }
                } else if (token instanceof PipeToken) {
                    waitingForSuffix = true;
                } else if (((BraceToken) token).isClosing()) {
                    currentNode = unclosedNodes.pop();
                } else {
                    if (currentNode == null) {
                        throw new SyntaxError(
                                "Type open brace (<) cannot go before the type name");
                    }
                    unclosedNodes.push(currentNode);
                }
            }

            return currentNode;
        }

        public void addNested(final Node node) {
            nested.add(node);
        }

        @Override
        public Node clone() {
            Node clone = new Node(name);
            clone.setNested(nested.stream().map(Node::clone)
                    .collect(Collectors.toList()));
            clone.setUndefined(undefined);

            return clone;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public List<Node> getNested() {
            return nested;
        }

        public void setNested(List<Node> nested) {
            this.nested = nested;
        }

        public boolean hasNested() {
            return nested.size() > 0;
        }

        public boolean isUndefined() {
            return undefined;
        }

        public void setUndefined(final boolean undefined) {
            this.undefined = undefined;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);

            if (nested.size() > 0) {
                builder.append('<');
                builder.append(nested.stream().map(Node::toString)
                        .collect(Collectors.joining(", ")));
                builder.append('>');
            }

            if (undefined) {
                builder.append(" | undefined");
            }

            return builder.toString();
        }

        public Traverse traverse() {
            return new Traverse(this);
        }
    }

    static class PipeToken extends Token {
    }

    static class SyntaxError extends Error {
        public SyntaxError() {
            super();
        }

        public SyntaxError(String message) {
            super(message);
        }

        public SyntaxError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    abstract static class Token {
        private static final char CLOSE_BRACE = '>';
        private static final char COMMA = ',';
        private static final char OPEN_BRACE = '<';
        private static final char PIPE = '|';
        private static final char SPACE = ' ';

        static List<Token> process(CharSequence sequence) {
            List<Token> tokens = new ArrayList<>();
            NameToken token = null;

            for (int i = 0; i < sequence.length(); i++) {
                char ch = sequence.charAt(i);

                switch (ch) {
                case SPACE:
                    break;
                case PIPE:
                    tokens.add(new PipeToken());
                    token = null;
                    break;
                case OPEN_BRACE:
                    tokens.add(new BraceToken(false));
                    token = null;
                    break;
                case CLOSE_BRACE:
                    tokens.add(new BraceToken(true));
                    token = null;
                    break;
                case COMMA:
                    token = null;
                    break;
                default:
                    if (token == null) {
                        token = new NameToken();
                        tokens.add(token);
                    }
                    token.append(ch);
                    break;
                }

            }

            return tokens;
        }
    }

    static class Traverse {
        private final Node node;
        private final List<BiFunction<Node, Optional<Node>, Node>> visitors = new ArrayList<>();

        public Traverse(Node node) {
            this.node = node;
        }

        public Node finish() {
            return visitImpl(applyVisitors(node, null));
        }

        public Traverse visit(BiFunction<Node, Optional<Node>, Node> visitor) {
            visitors.add(visitor);

            return this;
        }

        private Node applyVisitors(Node node, Node parent) {
            Node tmp = node;

            for (BiFunction<Node, Optional<Node>, Node> visitor : visitors) {
                tmp = visitor.apply(tmp, Optional.ofNullable(parent));
            }

            return tmp;
        }

        private Node visitImpl(Node node) {
            if (node.hasNested()) {
                node.setNested(
                        node.nested.stream().map(n -> applyVisitors(n, node))
                                .filter(Objects::nonNull).map(this::visitImpl)
                                .collect(Collectors.toList()));
            }

            return node;
        }
    }
}
