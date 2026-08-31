import { expect } from '@open-wc/testing';
import { PushConfiguration } from '../../../../../main/frontend/internal/client/communication/PushConfiguration';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { Reactive } from '../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { inertRegistry } from '../flow/stateTreeTestRegistry';

// PushConfiguration reads the root node's push configuration through a real
// StateTree, so the configuration is written as the server writes it: values set
// on the real map, whose change events drive the reactive push toggle.
function makeRegistry(values: Record<string, unknown>) {
  const setPushCalls: boolean[] = [];
  const tree = new StateTree(inertRegistry());
  const configMap = tree.getRootNode().getMap(NodeFeatures.UI_PUSHCONFIGURATION);

  const parametersNode = new StateNode(3, tree);
  tree.registerNode(parametersNode);
  parametersNode
    .getMap(NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS)
    .getProperty('transports')
    .setValue('websocket,long-polling');
  configMap.getProperty('parameters').setValue(parametersNode);

  Object.entries(values).forEach(([key, value]) => configMap.getProperty(key).setValue(value));

  return {
    setPushCalls,
    configMap,
    getStateTree: () => tree,
    getMessageSender: () => ({ setPushEnabled: (enabled: boolean) => setPushCalls.push(enabled) })
  };
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
    registry.configMap.getProperty('pushMode').setValue('AUTOMATIC');
    expect(registry.setPushCalls).to.deep.equal([]); // deferred
    Reactive.flush();
    expect(registry.setPushCalls).to.deep.equal([true]);
  });

  it('disables push when the mode switches off', () => {
    const registry = makeRegistry({ pushMode: 'AUTOMATIC' });
    new PushConfiguration(registry);
    registry.configMap.getProperty('pushMode').setValue('DISABLED');
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
