package com.vaadin.base.devserver.themeeditor.messages;

import java.util.List;

public class ClassNamesRequest extends BaseRequest {

    private List<String> add;

    private List<String> remove;

    public ClassNamesRequest() {

    }

    public List<String> getAdd() {
        return add;
    }

    public void setAdd(List<String> add) {
        this.add = add;
    }

    public List<String> getRemove() {
        return remove;
    }

    public void setRemove(List<String> remove) {
        this.remove = remove;
    }
}
