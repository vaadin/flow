package com.vaadin.hummingbird.kernel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class IfElementTemplate extends BoundElementTemplateWithChildren {

    private final Predicate<StateNode> predicate;
    private final List<BoundElementTemplate> trueChildren;
    private final List<BoundElementTemplate> falseChildren;

    public IfElementTemplate(String tag, List<AttributeBinding> boundAttributes,
            Map<String, String> defaultAttributes,
            Predicate<StateNode> predicate,
            List<BoundElementTemplate> trueChildren,
            List<BoundElementTemplate> falseChildren) {
        super(tag, boundAttributes, defaultAttributes);
        this.predicate = predicate;
        this.trueChildren = trueChildren;
        this.falseChildren = falseChildren;

        trueChildren
                .forEach(c -> c.setParentResolver(createParentResolver(true)));
        falseChildren
                .forEach(c -> c.setParentResolver(createParentResolver(false)));
    }

    private Function<StateNode, Element> createParentResolver(boolean value) {
        return node -> {
            if (predicate.test(node) == value) {
                return Element.getElement(this, node);
            } else {
                return null;
            }
        };
    }

    private List<BoundElementTemplate> getChildList(StateNode node) {
        if (predicate.test(node)) {
            return trueChildren;
        } else {
            return falseChildren;
        }
    }

    @Override
    protected int doGetChildCount(StateNode node) {
        return getChildList(node).size();
    }

    @Override
    protected StateNode getChildNode(int childIndex, StateNode node) {
        return node;
    }

    @Override
    protected ElementTemplate getChildTemplate(int childIndex, StateNode node) {
        return getChildList(node).get(childIndex);
    }
}
