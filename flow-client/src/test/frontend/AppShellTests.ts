const {beforeEach, describe, it} = intern.getPlugin('interface.bdd');
const {assert} = intern.getPlugin("chai");

import "../../main/resources/META-INF/resources/frontend/ConnectionIndicator";
import {
  setConnectionIndicatorConfiguration,
} from "../../main/resources/META-INF/resources/frontend/AppShell";

const $wnd = window as any;

describe('AppShell', () => {

  beforeEach(async () => {
    recreateConnectionIndicator();
  });

  describe('setConnectionIndicatorConfiguration', () => {

    it('should configure indicator properties with defined fields and leave other properties undefined ', async () => {
      const defaultOfflineText = $wnd.Vaadin.connectionIndicator.offlineText;
      const defaultFirstDelay = $wnd.Vaadin.connectionIndicator.firstDelay;
      const defaultThirdDelay = $wnd.Vaadin.connectionIndicator.thirdDelay;
      const defaultReconnectModal = $wnd.Vaadin.connectionIndicator.reconnectModal;
      const defaultReconnectingText = $wnd.Vaadin.connectionIndicator.reconnectingText;

      const newConf = setConnectionIndicatorConfiguration({
        onlineText: 'You are online',
        secondDelay: 600,
        applyDefaultTheme: false
      });

      assert.equal($wnd.Vaadin.connectionIndicator.offlineText, defaultOfflineText);
      assert.equal(newConf.offlineText, defaultOfflineText);

      assert.equal($wnd.Vaadin.connectionIndicator.onlineText, 'You are online');
      assert.equal(newConf.onlineText, 'You are online');

      assert.equal($wnd.Vaadin.connectionIndicator.firstDelay, defaultFirstDelay);
      assert.equal(newConf.firstDelay, defaultFirstDelay);

      assert.equal($wnd.Vaadin.connectionIndicator.secondDelay, 600);
      assert.equal(newConf.secondDelay, 600);

      assert.equal($wnd.Vaadin.connectionIndicator.thirdDelay, defaultThirdDelay);
      assert.equal(newConf.thirdDelay, defaultThirdDelay);

      assert.isFalse($wnd.Vaadin.connectionIndicator.applyDefaultTheme);
      assert.isFalse(newConf.applyDefaultTheme);

      assert.equal($wnd.Vaadin.connectionIndicator.reconnectModal, defaultReconnectModal);
      assert.equal(newConf.reconnectModal, defaultReconnectModal);

      assert.equal($wnd.Vaadin.connectionIndicator.reconnectingText, defaultReconnectingText);
      assert.equal(newConf.reconnectingText, defaultReconnectingText);
    });

    it('should create a connection indicator in the DOM if not available', async () => {
      setConnectionIndicatorConfiguration({firstDelay: 123});

      assert.isDefined($wnd.Vaadin?.connectionIndicator);
      assert.equal($wnd.Vaadin.connectionIndicator.firstDelay, 123);
    });

    it('should reject negative delays', async () => {
      let error = undefined;
      try {
        setConnectionIndicatorConfiguration({firstDelay: -1});
      } catch (err) {
        error = err;
      }
      assert.isDefined(error);
    });
  });

});

async function recreateConnectionIndicator() {
  const indicator = $wnd.document.body.querySelector('vaadin-connection-indicator');
  if (indicator) {
    $wnd.document.body.removeChild(indicator);
  }
  delete $wnd.Vaadin;
  $wnd.Vaadin = {
    connectionIndicator: document.createElement('vaadin-connection-indicator')
  };
  document.body.appendChild($wnd.Vaadin.connectionIndicator);
  await $wnd.Vaadin.connectionIndicator.updateComplete;
}
