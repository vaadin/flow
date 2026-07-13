import { expect } from '@open-wc/testing';
import { LoadingIndicatorStateHandler } from '../../main/frontend/internal/communication/LoadingIndicatorStateHandler';

type Win = { Vaadin?: { connectionState?: unknown } };

function makeHandler(hasActiveRequest = false) {
  const calls: string[] = [];
  (window as Win).Vaadin = {
    connectionState: {
      loadingStarted: () => calls.push('started'),
      loadingFinished: () => calls.push('finished')
    }
  };
  const registry = { getRequestResponseTracker: () => ({ hasActiveRequest: () => hasActiveRequest }) };
  return { handler: new LoadingIndicatorStateHandler(registry), calls };
}

describe('LoadingIndicatorStateHandler', () => {
  afterEach(() => {
    delete (window as Win).Vaadin;
  });

  it('does not show loading for a silent (high-frequency) event request', () => {
    const { handler, calls } = makeHandler();
    handler.processMessage('event', 'mousemove');
    handler.startLoading();
    expect(calls).to.deep.equal([]);
  });

  it('shows loading for a non-silent event request', () => {
    const { handler, calls } = makeHandler();
    handler.processMessage('event', 'click');
    handler.startLoading();
    expect(calls).to.deep.equal(['started']);
  });

  it('shows loading for a non-event request type', () => {
    const { handler, calls } = makeHandler();
    handler.processMessage('mSync', null);
    handler.startLoading();
    expect(calls).to.deep.equal(['started']);
  });

  it('stops loading (debounced) when no request is active', async () => {
    const { handler, calls } = makeHandler(false);
    handler.processMessage('mSync', null);
    handler.startLoading();
    expect(calls).to.deep.equal(['started']);

    handler.stopLoading();
    expect(calls).to.deep.equal(['started']); // deferred
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(calls).to.deep.equal(['started', 'finished']);
  });

  it('does not stop loading while a request is still active', async () => {
    const { handler, calls } = makeHandler(true);
    handler.processMessage('mSync', null);
    handler.startLoading();
    handler.stopLoading();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(calls).to.deep.equal(['started']);
  });
});
