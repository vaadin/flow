/*
 * Copyright 2000-2023 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import {css, html, LitElement, nothing} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/button';
import '@vaadin/checkbox';
import {CheckboxCheckedChangedEvent} from "@vaadin/checkbox";

@customElement('web-push-toggle')
export class WebPushToggle extends LitElement {
  @property({type: String}) caption = 'Subscribe to push notifications';
  @property({type: String}) publicKey = '';
  @state() denied = Notification.permission === 'denied';
  @state() subscribed = false;

  static styles = css`
    :host {
      display: block;
    }
  `;

  render() {
    return html`
        ${this.denied
                ? html` <b> You have blocked notifications. You need to manually enable them in your browser. </b> `
                : nothing}
        <vaadin-checkbox
                ?checked=${this.subscribed}
                label=${this.caption}
                @checked-changed=${this.updateSubscription}
        ></vaadin-checkbox>

    `;
  }

  first = true;
  updateSubscription(e: CheckboxCheckedChangedEvent){

    // The checkbox fires an event on initialization, ignore it.
    if(this.first){
      this.first = false;
      return;
    }

    // The checkbox value is set on initialization based on the service worker subscription.
    // Don't re-subscribe if we're already subscribed.
    if(e.detail.value === this.subscribed) {
      return;
    }
    if(e.detail.value){
      this.subscribe();
    } else {
      this.unsubscribe();
    }
  }

  async connectedCallback() {
    super.connectedCallback();
    const registration = await navigator.serviceWorker.getRegistration();
    this.subscribed = !!(await registration?.pushManager.getSubscription());
  }

  async subscribe() {
    const notificationPermission = await Notification.requestPermission();

    if (notificationPermission === 'granted') {
      const registration = await navigator.serviceWorker.getRegistration();
      const subscription = await registration?.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlB64ToUint8Array(this.publicKey),
      });

      if (subscription) {
        this.subscribed = true;
        console.log(subscription);
        console.log(JSON.parse(JSON.stringify(subscription)));
        this.dispatchEvent(new CustomEvent('web-push-subscribed', {
          bubbles: true,
          composed: true,
          // Serialize keys uint8array -> base64
          detail: JSON.parse(JSON.stringify(subscription))
        }));
      }
    } else {
      this.denied = true;
    }
  }

  async unsubscribe() {
    const registration = await navigator.serviceWorker.getRegistration();
    const subscription = await registration?.pushManager.getSubscription();
    if (subscription) {
      await subscription.unsubscribe();

      this.dispatchEvent(new CustomEvent('web-push-unsubscribed', {
        bubbles: true,
        composed: true,
        // Serialize keys uint8array -> base64
        detail: JSON.parse(JSON.stringify(subscription))
      }));

      this.subscribed = false;
    }
  }

  private urlB64ToUint8Array(base64String: string) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/\-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }
}
