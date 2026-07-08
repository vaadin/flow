import { expect } from '@open-wc/testing';
import { PushConfiguration } from '../../main/frontend/internal/communication/PushConfiguration';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';

function makeRegistry(values: Record<string, unknown>) {
  const setPushCalls: boolean[] = [];
  let pushModeListener: ((event: { getOldValue(): unknown; getNewValue(): unknown }) => void) | undefined;

  const parametersNode = {
    getMap: () => ({
      getProperty: () => ({ getValue: () => undefined, addChangeListener: () => {} }),
      hasPropertyValue: () => false,
      forEachProperty: (cb: (property: { getValue(): unknown }, key: string) => void) => {
        cb({ getValue: () => 'websocket,long-polling' }, 'transports');
      }
    })
  };

  const configMap = {
    getProperty: (key: string) => {
      if (key === 'pushMode') {
        return {
          getValue: () => values.pushMode,
          addChangeListener: (l: (event: { getOldValue(): unknown; getNewValue(): unknown }) => void) => {
            pushModeListener = l;
          }
        };
      }
      if (key === 'parameters') {
        return { getValue: () => parametersNode, addChangeListener: () => {} };
      }
      return { getValue: () => values[key], addChangeListener: () => {} };
    },
    hasPropertyValue: (key: string) => values[key] !== undefined,
    forEachProperty: () => {}
  };

  const registry = {
    setPushCalls,
    firePushModeChange: (oldValue: unknown, newValue: unknown) =>
      pushModeListener?.({ getOldValue: () => oldValue, getNewValue: () => newValue }),
    getStateTree: () => ({ getRootNode: () => ({ getMap: () => configMap }) }),
    getMessageSender: () => ({ setPushEnabled: (enabled: boolean) => setPushCalls.push(enabled) })
  };
  return registry;
}

describe('PushConfiguration', () => {
  afterEach(() => Reactive.flush());

  it('reports whether push is enabled from the push mode', () => {
    expect(new PushConfiguration(makeRegistry({ pushMode: 'AUTOMATIC' })).isPushEnabled()).to.be.true;
    expect(new PushConfiguration(makeRegistry({ pushMode: 'DISABLED' })).isPushEnabled()).to.be.false;
    expect(new PushConfiguration(makeRegistry({})).isPushEnabled()).to.be.false;
  });

  it('enables push (deferred to flush) when the mode switches on', () => {
    const registry = makeRegistry({ pushMode: 'DISABLED' });
    new PushConfiguration(registry);
    registry.firePushModeChange('DISABLED', 'AUTOMATIC');
    expect(registry.setPushCalls).to.deep.equal([]); // deferred
    Reactive.flush();
    expect(registry.setPushCalls).to.deep.equal([true]);
  });

  it('disables push when the mode switches off', () => {
    const registry = makeRegistry({ pushMode: 'AUTOMATIC' });
    new PushConfiguration(registry);
    registry.firePushModeChange('AUTOMATIC', 'DISABLED');
    Reactive.flush();
    expect(registry.setPushCalls).to.deep.equal([false]);
  });

  it('exposes servlet mapping, always-xhr and parameters', () => {
    const registry = makeRegistry({
      pushMode: 'AUTOMATIC',
      pushServletMapping: '/vaadinPush/',
      alwaysXhrToServer: true
    });
    const config = new PushConfiguration(registry);
    expect(config.getPushServletMapping()).to.equal('/vaadinPush/');
    expect(config.isAlwaysXhrToServer()).to.be.true;
    expect(config.getParameters().get('transports')).to.equal('websocket,long-polling');

    const noMapping = new PushConfiguration(makeRegistry({ pushMode: 'AUTOMATIC' }));
    expect(noMapping.getPushServletMapping()).to.equal(null);
    expect(noMapping.isAlwaysXhrToServer()).to.be.false;
  });
});
