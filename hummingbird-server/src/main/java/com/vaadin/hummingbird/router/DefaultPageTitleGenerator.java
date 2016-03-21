package com.vaadin.hummingbird.router;

import com.vaadin.annotations.Title;

/**
 * The default page title generator.
 * <p>
 * Uses {@link View}'s {@link Title#value()} or
 * {@link View#getTitle(LocationChangeEvent)}.
 * <p>
 * Can be replaced with
 * {@link ModifiableRouterConfiguration#setPageTitleGenerator(PageTitleGenerator)}
 * .
 */
public class DefaultPageTitleGenerator implements PageTitleGenerator {

    @Override
    public String getPageTitle(LocationChangeEvent event,
            ViewRenderer viewRenderer) {
        return event.getViewChain().get(0).getTitle(event);
    }

}
