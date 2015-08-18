package hummingbird.todonotemplate;

import com.vaadin.annotations.Tag;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.NativeButton;

@Tag("footer")
public class TodoFooter extends CssLayout {

    public enum Filter {
        ALL, COMPLETED, ACTIVE;

        public boolean pass(boolean completed) {
            if (this == ALL) {
                return true;
            } else if (this == COMPLETED) {
                return completed;
            } else {
                return !completed;
            }
        }
    }

    private HTML todoCount;
    private NativeButton filterAll;
    private NativeButton filterActive;
    private NativeButton filterCompleted;
    private NativeButton clearCompleted;

    private TodoPresenter getPresenter() {
        return getView().getPresenter();
    }

    private TodoViewImpl getView() {
        return findAncestor(TodoViewImpl.class);
    }

    public TodoFooter() {
        setId("footer");

        todoCount = new HTML("<span><b>0</b> items left</span>");
        todoCount.setId("todo-count");
        addComponent(todoCount);

        CssLayout filters = new CssLayout();
        filters.setId("filters");
        addComponent(filters);

        filterAll = new NativeButton("All");
        filterActive = new NativeButton("Active");
        filterCompleted = new NativeButton("Completed");

        filterAll.addStyleName("selected");
        filters.addComponents(filterAll, filterActive, filterCompleted);

        clearCompleted = new NativeButton("Clear completed (0)");
        clearCompleted.setId("clear-completed");
        addComponent(clearCompleted);

        filterAll.addClickListener((e) -> filter(Filter.ALL));
        filterActive.addClickListener((e) -> filter(Filter.ACTIVE));
        filterCompleted.addClickListener((e) -> filter(Filter.COMPLETED));

        clearCompleted.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                getPresenter().clearCompleted();
            }
        });
    }

    protected void filter(Filter filter) {
        getView().setFilter(filter);
        filterAll.setStyleName("selected", filter == Filter.ALL);
        filterActive.setStyleName("selected", filter == Filter.ACTIVE);
        filterCompleted.setStyleName("selected", filter == Filter.COMPLETED);
    }

    public void updateCounters(int completed, int remaining) {
        todoCount.setInnerHtml("<b>" + remaining + "</b> items left");
        clearCompleted.setCaption("Clear completed (" + completed + ")");
        clearCompleted.setVisible(completed != 0);

    }
}