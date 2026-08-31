// Beyond the Java suite: LoadingIndicatorConfigurator has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { expect } from '@open-wc/testing';
import { observe as observeLoadingIndicator } from '../../../../../main/frontend/internal/client/communication/LoadingIndicatorConfigurator';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { Reactive } from '../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { inertNode } from '../flow/stateTreeTestRegistry';

describe('LoadingIndicatorConfigurator', () => {
  afterEach(() => {
    delete (window as { Vaadin?: unknown }).Vaadin;
    Reactive.flush();
  });

  it('applies delay and theme properties to the connection indicator', () => {
    const indicator: Record<string, unknown> = {};
    (window as { Vaadin?: unknown }).Vaadin = { connectionIndicator: indicator };

    const node = inertNode();
    const configMap = node.getMap(NodeFeatures.LOADING_INDICATOR_CONFIGURATION);
    observeLoadingIndicator(node);

    configMap.getProperty('first').setValue(100);
    configMap.getProperty('second').setValue(200);
    configMap.getProperty('third').setValue(300);
    configMap.getProperty('theme').setValue(false);

    expect(indicator).to.deep.equal({
      firstDelay: 100,
      secondDelay: 200,
      thirdDelay: 300,
      applyDefaultTheme: false
    });
  });

  it('falls back to the default when a property is cleared', () => {
    const indicator: Record<string, unknown> = {};
    (window as { Vaadin?: unknown }).Vaadin = { connectionIndicator: indicator };

    const node = inertNode();
    const configMap = node.getMap(NodeFeatures.LOADING_INDICATOR_CONFIGURATION);
    observeLoadingIndicator(node);

    configMap.getProperty('first').setValue(100);
    configMap.getProperty('first').removeValue();

    expect(indicator.firstDelay).to.equal(450);
  });
});
