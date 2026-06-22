import { expect } from '@open-wc/testing';
import {
  calculateBootstrapTime,
  callAfterServerUpdates,
  getFetchStartTime,
  parseJSONResponse,
  removeStylesheetByIdFromDom
} from '../../main/frontend/internal/MessageHandler';

describe('MessageHandler', () => {
  it('removeStylesheetByIdFromDom removes link and style elements by data-id', () => {
    const link = document.createElement('link');
    link.setAttribute('data-id', 'dep-x');
    const style = document.createElement('style');
    style.setAttribute('data-id', 'dep-x');
    const keep = document.createElement('style');
    keep.setAttribute('data-id', 'dep-y');
    document.head.append(link, style, keep);

    removeStylesheetByIdFromDom('dep-x');

    expect(document.querySelector('[data-id="dep-x"]')).to.equal(null);
    expect(document.querySelector('[data-id="dep-y"]')).to.not.equal(null);
    keep.remove();
  });

  it('callAfterServerUpdates invokes afterServerUpdate when present', () => {
    let called = false;
    const node = {
      afterServerUpdate: () => {
        called = true;
      }
    } as unknown as Node;
    callAfterServerUpdates(node);
    expect(called).to.be.true;
  });

  it('callAfterServerUpdates is a no-op without the callback', () => {
    expect(() => callAfterServerUpdates(document.createElement('div'))).to.not.throw();
  });

  it('parseJSONResponse parses JSON text', () => {
    expect(parseJSONResponse('{"a":1,"b":"x"}')).to.eql({ a: 1, b: 'x' });
  });

  it('calculateBootstrapTime and getFetchStartTime return numbers', () => {
    expect(calculateBootstrapTime()).to.be.a('number');
    expect(getFetchStartTime()).to.be.a('number');
  });
});
