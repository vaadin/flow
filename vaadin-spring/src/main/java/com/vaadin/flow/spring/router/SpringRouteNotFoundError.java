package com.vaadin.flow.spring.router;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteNotFoundError;

public class SpringRouteNotFoundError extends RouteNotFoundError {
    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        int retval = super.setErrorParameter(event, parameter);

        if (!event.getUI().getSession().getConfiguration().isProductionMode()) {
            // Alert user about potential issue with Spring Boot Devtools losing
            // routes https://github.com/spring-projects/spring-boot/issues/19543
            String customMessage = "<span>When using Spring Boot Devtools with "
                    + "automatic reload, please note that routes can sometimes be "
                    + "lost due to a <a href ='https://github.com/spring-projects/spring-boot/issues/19543'>"
                    + "compilation race condition</a>. See "
                    + "<a href='https://vaadin.com/docs/flow/workflow/setup-live-reload-springboot.html'>"
                    + "the documentation</a> for further workarounds and other "
                    + "live reload alternatives.";
            getElement().appendChild(new Html(customMessage).getElement());
        }
        return retval;
    }
}
