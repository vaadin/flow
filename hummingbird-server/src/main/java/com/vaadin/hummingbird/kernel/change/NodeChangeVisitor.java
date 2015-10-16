package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public interface NodeChangeVisitor {

    void visitIdChange(StateNode node, IdChange idChange);

    void visitListInsertChange(StateNode node,
            ListInsertChange listInsertChange);

    void visitListInsertManyChange(StateNode node,
            ListInsertManyChange listInsertManyChange);

    void visitListRemoveChange(StateNode node,
            ListRemoveChange listRemoveChange);

    void visitListReplaceChange(StateNode node,
            ListReplaceChange listReplaceChange);

    void visitParentChange(StateNode node, ParentChange parentChange);

    void visitPutChange(StateNode node, PutChange putChange);

    void visitRemoveChange(StateNode node, RemoveChange removeChange);

}
