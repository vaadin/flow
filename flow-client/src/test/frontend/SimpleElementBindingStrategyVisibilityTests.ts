import { expect } from '@open-wc/testing';
import {
  applyStructuralAttributes,
  restoreInitialHiddenAttribute,
  setElementInvisible,
  storeInitialHiddenAttribute
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

const ELEMENT_ATTRIBUTES = 3;

// A stateful MapProperty stand-in.
function fakeProperty(initial?: unknown) {
  let value = initial;
  let has = arguments.length > 0;
  return {
    hasValue: () => has,
    getValue: () => value,
    setValue: (v: unknown) => {
      value = v;
      has = true;
    }
  };
}

// A visibility-data NodeMap stand-in: named properties plus the config chain.
function fakeVisibilityData(properties: Record<string, ReturnType<typeof fakeProperty>>, webComponentMode = false) {
  const configuration = { isWebComponentMode: () => webComponentMode, getServiceUrl: () => '' };
  return {
    getProperty: (name: string) => {
      properties[name] ??= fakeProperty();
      return properties[name];
    },
    getNode: () => ({
      getTree: () => ({ getRegistry: () => ({ getApplicationConfiguration: () => configuration }) })
    })
  };
}

// Attaches an element inside an open shadow root so isInShadowRoot is true.
function inShadowRoot<T extends Element>(element: T): T {
  const host = document.createElement('div');
  host.attachShadow({ mode: 'open' }).appendChild(element);
  return element;
}

describe('SimpleElementBindingStrategy visibility binding', () => {
  it('storeInitialHiddenAttribute captures the hidden attribute once', () => {
    const element = document.createElement('div');
    element.setAttribute('hidden', 'true');
    const hidden = fakeProperty();
    storeInitialHiddenAttribute(element, fakeVisibilityData({ hidden }));
    expect(hidden.getValue()).to.equal('true');

    // A second call does not overwrite the captured value.
    element.removeAttribute('hidden');
    storeInitialHiddenAttribute(element, fakeVisibilityData({ hidden }));
    expect(hidden.getValue()).to.equal('true');
  });

  it('storeInitialHiddenAttribute captures inline display only inside a shadow root', () => {
    const element = inShadowRoot(document.createElement('div'));
    element.style.display = 'flex';
    const styleDisplay = fakeProperty();
    storeInitialHiddenAttribute(element, fakeVisibilityData({ styleDisplay }));
    expect(styleDisplay.getValue()).to.equal('flex');
  });

  it('setElementInvisible sets the hidden attribute and shadow-root display:none', () => {
    const element = inShadowRoot(document.createElement('div'));
    setElementInvisible(element, fakeVisibilityData({}));
    expect(element.getAttribute('hidden')).to.equal('true');
    expect((element as HTMLElement).style.display).to.equal('none');
  });

  it('restoreInitialHiddenAttribute re-applies the captured hidden attribute', () => {
    const element = document.createElement('div');
    element.setAttribute('hidden', 'true');
    const data = fakeVisibilityData({});
    storeInitialHiddenAttribute(element, data);

    setElementInvisible(element, data);
    expect(element.getAttribute('hidden')).to.equal('true');

    // Capture a "no hidden attribute" initial state and confirm restore removes it.
    const element2 = document.createElement('div');
    const data2 = fakeVisibilityData({});
    storeInitialHiddenAttribute(element2, data2); // captures null (no hidden attr)
    element2.setAttribute('hidden', 'true');
    restoreInitialHiddenAttribute(element2, data2);
    expect(element2.hasAttribute('hidden')).to.be.false;
  });

  it('applyStructuralAttributes sets the slot attribute when present', () => {
    const element = document.createElement('div');
    const slotProperty = {
      getName: () => 'slot',
      getValue: () => 'header',
      getMap: () => ({
        getNode: () => ({
          getTree: () => ({
            getRegistry: () => ({
              getApplicationConfiguration: () => ({ isWebComponentMode: () => false, getServiceUrl: () => '' })
            })
          })
        })
      })
    };
    const node = {
      hasFeature: (feature: number) => feature === ELEMENT_ATTRIBUTES,
      getMap: (_feature: number) => ({
        hasPropertyValue: (name: string) => name === 'slot',
        getProperty: (_name: string) => slotProperty
      })
    };
    applyStructuralAttributes(node, element);
    expect(element.getAttribute('slot')).to.equal('header');
  });
});
