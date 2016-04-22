package com.vaadin.humminbird.tutorial;

import java.util.EventObject;
import java.util.function.Consumer;

import com.vaadin.annotations.Tag;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.ui.Component;

@CodeFor("tutorial-component-with-dependencies.asciidoc")
public class ComponentAttach {

    interface I18N {
        static String getTranslation(String localeName, String key,
                String... params) {
            return "";
        }
    }

    interface ShopEventBus {
        void register(Consumer<EventObject> eventHandler);

        void unregister(Consumer<EventObject> eventHandler);
    }

    @Tag("div")
    public class UserNameLabel extends Component {

        @Override
        protected void onAttach() {
            // user name can be stored to session after login
            String userName = (String) getUI().get().getSession()
                    .getAttribute("username");
            // user's selected locale can be stored to session
            String locale = (String) getUI().get().getSession()
                    .getAttribute("userLocale");

            // I18n is an utility for fetching localized strings
            String localizedHelloText = I18N.getTranslation(locale,
                    "welcome.user.label", userName);
            getElement().setTextContent(localizedHelloText);
        }
    }

    @Tag("div")
    public class ShoppingCartSummaryLabel extends Component {

        @Override
        protected void onAttach() {
            ShopEventBus eventBus = getUI().get().getSession()
                    .getAttribute(ShopEventBus.class);
            // registering to event bus for updates from other components
            eventBus.register(this::onCartSummaryUpdate);
        }

        @Override
        protected void onDetach() {
            ShopEventBus eventBus = getUI().get().getSession()
                    .getAttribute(ShopEventBus.class);
            // after detaching don't need any updates
            eventBus.unregister(this::onCartSummaryUpdate);
        }

        private void onCartSummaryUpdate(EventObject event) {
            // update cart summary ...
        }
    }
}
