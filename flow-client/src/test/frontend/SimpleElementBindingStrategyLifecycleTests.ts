import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import {
  BindingContext,
  doBind,
  InitialPropertyUpdate,
  remove,
  scheduleInitialExecution
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

describe('SimpleElementBindingStrategy bind lifecycle', () => {
  it('doBind re-sets the DOM node and rebinds', () => {
    const dom = document.createElement('div');
    const calls: Array<Node | null> = [];
    const bound: unknown[] = [];
    const node: any = {
      getDomNode: () => dom,
      setDomNode: (n: Node | null) => calls.push(n)
    };
    const nodeFactory: any = { createAndBind: (n: unknown) => bound.push(n) };
    doBind(node, nodeFactory);
    // null first (re-fires the event), then the original dom node back.
    expect(calls).to.deep.equal([null, dom]);
    expect(bound).to.deep.equal([node]);
  });

  it('scheduleInitialExecution stores and later runs the initial update', async () => {
    let stored: InitialPropertyUpdate | null = null;
    const ran: string[] = [];
    const node: any = {
      getDomNode: () => null,
      setDomNode: () => {},
      setNodeData: (o: any) => {
        stored = o;
      },
      getNodeData: (clazz: unknown) => (clazz === InitialPropertyUpdate ? stored : null),
      clearNodeData: () => {
        stored = null;
      }
    };
    // Give the stored update a command to observe execution.
    scheduleInitialExecution(node);
    expect(stored).to.be.instanceOf(InitialPropertyUpdate);
    stored!.setCommand(() => ran.push('init'));

    Reactive.flush(); // runs the post-flush listener which schedules the deferred
    await new Promise((resolve) => setTimeout(resolve, 0)); // let the deferred run
    expect(ran).to.deep.equal(['init']);
  });

  it('remove stops computations and removes listeners', () => {
    const stopped: string[] = [];
    const removed: string[] = [];
    const element = document.createElement('div');
    const context = new BindingContext({} as any, element, {} as any);
    context.listenerBindings.set('a', { stop: () => stopped.push('a') } as any);
    context.listenerRemovers.set('click', { remove: () => removed.push('click') });
    const computationsCollection = [new Map<string, any>([['b', { stop: () => stopped.push('b') }]])];
    const listeners = [{ remove: () => removed.push('listener') }];

    remove(listeners, context, computationsCollection);
    expect(stopped.sort()).to.deep.equal(['a', 'b']);
    expect(removed.sort()).to.deep.equal(['click', 'listener']);
  });
});
