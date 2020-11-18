const {beforeEach, describe, it} = intern.getPlugin('interface.bdd');
const {assert} = intern.getPlugin("chai");

import "../../main/resources/META-INF/resources/frontend/ConnectionIndicator";
import {
    setConnectionIndicatorConfiguration,
} from "../../main/resources/META-INF/resources/frontend/AppShell";

const $wnd = window as any;

describe('AppShell', () => {

    beforeEach(async () => {
        createConnectionIndicator();
    });

    describe('setConnectionIndicatorConfiguration', () => {

        it('should configure indicator properties with defined fields and leave other properties undefined ', async () => {
            const defaultOfflineText = $wnd.Vaadin.connectionIndicator.offlineText;
            const defaultFirstDelay = $wnd.Vaadin.connectionIndicator.firstDelay;
            const defaultThirdDelay = $wnd.Vaadin.connectionIndicator.thirdDelay;
            const defaultReconnectModal = $wnd.Vaadin.connectionIndicator.reconnectModal;
            const defaultReconnectingText = $wnd.Vaadin.connectionIndicator.reconnectingText;

            const newConf = await setConnectionIndicatorConfiguration({
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

        it('should defer configuration if indicator not available', async () => {
            delete $wnd.Vaadin.connectionIndicator;

            setTimeout(() => createConnectionIndicator(), 100);

            await setConnectionIndicatorConfiguration({firstDelay: 123});
            assert.equal($wnd.Vaadin.connectionIndicator.firstDelay, 123);
        });

        it('should reject negative delays', async () => {
            let error = undefined;
            await setConnectionIndicatorConfiguration({firstDelay: -1}).catch(err => {
                error = err;
            });
            assert.isDefined(error)
        });
    });

});

async function createConnectionIndicator() {
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
