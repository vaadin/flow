package com.vaadin.flow.notification;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.templatemodel.TemplateModel;

import java.util.concurrent.CompletableFuture;

class PushNotificationHelper {


  public PushNotificationHelper(String vapidPubKey) {
    UI.getCurrent().getPage().addJavaScript("frontend://push-notification-helper.js");

    UI.getCurrent().getPage().executeJavaScript("window.Vaadin.pushHelper.vapidPubKey='" + vapidPubKey+"';");
  }

  public CompletableFuture<Boolean> browserSupportsPushNotifications() {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    //TODO: unregister listener
    UI.getCurrent().getElement().addEventListener("support-detected", e ->
        future.complete(e.getEventData().getBoolean("event.detail"))).addEventData("event.detail");
    UI.getCurrent().getPage().executeJavaScript("window.Vaadin.pushHelper.checkBrowserSupport()");
    return future;
  }

  public CompletableFuture<Boolean> notificationsEnabled() {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    //TODO: unregister listener
    UI.getCurrent().getElement().addEventListener("notification-status-detected", e ->
        future.complete(e.getEventData().getBoolean("event.detail"))).addEventData("event.detail");
    UI.getCurrent().getPage().executeJavaScript("window.Vaadin.pushHelper.checkNotificationStatus()");
    return future;
  }

  public CompletableFuture<String> subscribeToNotifications() {
    CompletableFuture<String> future = new CompletableFuture<>();
    //TODO: unregister listener
    UI.getCurrent().getElement().addEventListener("subscription-updated", e ->
        future.complete(e.getEventData().get("event.detail").toJson())).addEventData("event.detail");
    UI.getCurrent().getPage().executeJavaScript("window.Vaadin.pushHelper.subscribeToNotifications()");
    return future;
  }

  public void unsubscribeFromNotifications() {
    UI.getCurrent().getPage().executeJavaScript("window.Vaadin.pushHelper.unsubscribeFromNotifications()");
  }

  interface Model extends TemplateModel {
    void setVapidPubKey(String vapidPubKey);
  }

}
