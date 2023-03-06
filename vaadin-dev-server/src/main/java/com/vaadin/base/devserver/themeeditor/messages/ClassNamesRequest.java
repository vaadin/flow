package com.vaadin.base.devserver.themeeditor.messages;

import java.util.List;

public class ClassNamesRequest extends BaseRequest {

    public static final String COMMAND_NAME = "themeEditorClassNames";

    private List<String> add;

    private List<String> remove;

    public ClassNamesRequest() {

    }

    public ClassNamesRequest(String requestId, List<String> add,
            List<String> remove) {
        super(requestId);
        this.add = add;
        this.remove = remove;
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
