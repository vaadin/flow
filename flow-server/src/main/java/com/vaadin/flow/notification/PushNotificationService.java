package com.vaadin.flow.notification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vaadin.flow.component.UI;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.concurrent.CompletableFuture;

public class PushNotificationService {

  private PushService pushService;

  public PushNotificationService(String vapidPubKey, String vapidPrivateKey, String subject) {
    Security.addProvider(new BouncyCastleProvider());
    UI.getCurrent().getPage().addJavaScript("frontend://push-notification-helper.js");
    UI.getCurrent().getPage().executeJs("window.Vaadin.pushHelper.vapidPubKey='" + vapidPubKey+"';");
    try {
      pushService = new PushService(vapidPubKey, vapidPrivateKey, subject);
    } catch (GeneralSecurityException e) {
      e.printStackTrace();//TODO
    }
  }

  public CompletableFuture<Boolean> browserSupportsPushNotifications() {
    return UI.getCurrent().getPage().executeJs("return window.Vaadin.pushHelper.checkBrowserSupport()").toCompletableFuture(Boolean.class);
  }

  public CompletableFuture<Boolean> notificationsEnabled() {
    return UI.getCurrent().getPage().executeJs("return window.Vaadin.pushHelper.checkNotificationStatus()").toCompletableFuture(Boolean.class);
  }

  public CompletableFuture<String> subscribeToNotifications() {
    return UI.getCurrent().getPage().executeJs("return window.Vaadin.pushHelper.subscribeToNotifications()").toCompletableFuture(String.class);
  }

  public void unsubscribeFromNotifications() {
    UI.getCurrent().getPage().executeJs("window.Vaadin.pushHelper.unsubscribeFromNotifications()");
  }

  public void sendPushMessage(String subscription, String title, String body, String url){
    // Parse subscription JSON for details
    JsonObject subscriptionDetails = new JsonParser().parse(subscription).getAsJsonObject();
    String endpoint = subscriptionDetails.get("endpoint").getAsString();
    JsonObject keys = subscriptionDetails.getAsJsonObject("keys");
    String p256dh = keys.get("p256dh").getAsString();
    String auth = keys.get("auth").getAsString();

    // Construct JSON payload
    JsonObject payload = new JsonObject();
    payload.addProperty("title", title);
    payload.addProperty("body", body);
    payload.addProperty("url", url);

    // Either the API below is weird or I'm using it wrong. Keys should be a static class.
    Subscription sub = new Subscription(endpoint, new Subscription().new Keys(p256dh, auth));
    try {
      pushService.sendAsync(new Notification(sub, payload.toString()));
    } catch (GeneralSecurityException | IOException | JoseException e) {
      e.printStackTrace(); // TODO
    }
  }
}
