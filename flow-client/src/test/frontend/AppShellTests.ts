const {beforeEach, describe, it} = intern.getPlugin('interface.bdd');
const {assert} = intern.getPlugin("chai");

import "../../main/resources/META-INF/resources/frontend/LoadingIndicator";
import {
    setConnectionIndicatorConfiguration,
} from "../../main/resources/META-INF/resources/frontend/AppShell";

const $wnd = window as any;

describe('AppShell', () => {

    beforeEach(async () => {
        createIndicator();
    });

    describe('setConnectionIndicatorConfiguration', () => {

        it('should configure indicator with defined fields', async () => {
            const defaultFirstDelay = $wnd.Vaadin.loadingIndicator.firstDelay;
            const defaultThirdDelay = $wnd.Vaadin.loadingIndicator.thirdDelay;
            const newConf = await setConnectionIndicatorConfiguration({
                secondDelay: 600,
                applyDefaultTheme: false
            });

            assert.equal($wnd.Vaadin.loadingIndicator.firstDelay, defaultFirstDelay);
            assert.equal(newConf.firstDelay, defaultFirstDelay);

            assert.equal($wnd.Vaadin.loadingIndicator.secondDelay, 600);
            assert.equal(newConf.secondDelay, 600);

            assert.equal($wnd.Vaadin.loadingIndicator.thirdDelay, defaultThirdDelay);
            assert.equal(newConf.thirdDelay, defaultThirdDelay);

            assert.isFalse($wnd.Vaadin.loadingIndicator.applyDefaultTheme);
            assert.isFalse(newConf.applyDefaultTheme);
        });

        it('should defer configuration if indicator not available', async () => {
            delete $wnd.Vaadin.loadingIndicator;

            setTimeout(() => createIndicator(), 100);

            await setConnectionIndicatorConfiguration({firstDelay: 123});
            assert.equal($wnd.Vaadin.loadingIndicator.firstDelay, 123);
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

async function createIndicator() {
    const indicator = $wnd.document.body.querySelector('vaadin-loading-indicator');
    if (indicator) {
        $wnd.document.body.removeChild(indicator);
    }
    delete $wnd.Vaadin;
    $wnd.Vaadin = {
        loadingIndicator: document.createElement('vaadin-loading-indicator')
    };
    document.body.appendChild($wnd.Vaadin.loadingIndicator);
    await $wnd.Vaadin.loadingIndicator.updateComplete;
}
