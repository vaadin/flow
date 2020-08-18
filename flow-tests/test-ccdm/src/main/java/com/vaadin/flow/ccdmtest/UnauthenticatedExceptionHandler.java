package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

import javax.servlet.http.HttpServletResponse;


@Tag(Tag.DIV)
public class UnauthenticatedExceptionHandler
    extends Component
    implements HasErrorParameter<UnauthenticatedException>
{

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
                                 ErrorParameter<UnauthenticatedException>
                                     parameter) {
        setId("errorView");
        getElement().setText("Tried to navigate to a view without being authenticated");
        return HttpServletResponse.SC_UNAUTHORIZED;
    }
}
