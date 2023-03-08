package com.vaadin.base.devserver.themeeditor.utils;

import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;

@FunctionalInterface
public interface MessageHandlerCommand {

    BaseResponse execute();

}
