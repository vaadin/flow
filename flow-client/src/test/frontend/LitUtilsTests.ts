import { expect } from '@open-wc/testing';
import { isLitElement, whenRendered } from '../../main/frontend/internal/LitUtils';

describe('LitUtils', () => {
  it('isLitElement recognizes a LitElement by its API', () => {
    const litLike = {
      update() {},
      updateComplete: Promise.resolve(),
      shouldUpdate() {
        return true;
      },
      firstUpdated() {}
    } as unknown as Node;
    expect(isLitElement(litLike)).to.be.true;
  });

  it('isLitElement returns false for non-Lit elements', () => {
    expect(isLitElement(document.createElement('div'))).to.be.false;
    const missingOne = {
      update() {},
      updateComplete: Promise.resolve(),
      shouldUpdate() {
        return true;
      }
    } as unknown as Node;
    expect(isLitElement(missingOne)).to.be.false;
  });

  it('whenRendered runs the callback once the element has rendered', async () => {
    const element = { updateComplete: Promise.resolve() } as unknown as Element;
    await new Promise<void>((resolve) => {
      whenRendered(element, resolve);
    });
    // Reaching here means the callback was invoked; the test would otherwise time out.
    expect(true).to.be.true;
  });
});
