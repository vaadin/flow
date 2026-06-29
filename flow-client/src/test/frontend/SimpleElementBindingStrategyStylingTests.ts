import { expect } from '@open-wc/testing';
import { bindClassList, updateStyleProperty } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

function fakeStyleProperty(name: string, value: unknown, hasValue = true) {
  return {
    getName: () => name,
    hasValue: () => hasValue,
    getValue: () => value
  };
}

// A NodeList stand-in holding class names, with a hook to fire a splice event.
function fakeList(items: string[]) {
  const listeners: Array<(e: { getRemove(): unknown[]; getAdd(): unknown[] }) => void> = [];
  return {
    length: () => items.length,
    get: (i: number) => items[i],
    addSpliceListener(listener: (e: { getRemove(): unknown[]; getAdd(): unknown[] }) => void) {
      listeners.push(listener);
      return { remove: () => listeners.splice(listeners.indexOf(listener), 1) };
    },
    fireSplice(remove: string[], add: string[]) {
      listeners.forEach((l) => l({ getRemove: () => remove, getAdd: () => add }));
    }
  };
}

describe('SimpleElementBindingStrategy styling binding', () => {
  it('updateStyleProperty sets a plain style value', () => {
    const element = document.createElement('div');
    updateStyleProperty(fakeStyleProperty('color', 'blue'), element);
    expect(element.style.getPropertyValue('color')).to.equal('blue');
  });

  it('updateStyleProperty preserves an !important priority', () => {
    const element = document.createElement('div');
    updateStyleProperty(fakeStyleProperty('color', 'red !important'), element);
    expect(element.style.getPropertyValue('color')).to.equal('red');
    expect(element.style.getPropertyPriority('color')).to.equal('important');
  });

  it('updateStyleProperty removes the property when there is no value', () => {
    const element = document.createElement('div');
    element.style.setProperty('margin', '5px');
    updateStyleProperty(fakeStyleProperty('margin', undefined, false), element);
    expect(element.style.getPropertyValue('margin')).to.equal('');
  });

  it('bindClassList applies current classes and stays in sync on splice', () => {
    const element = document.createElement('div');
    const list = fakeList(['a', 'b']);
    const remover = bindClassList(element, { getList: () => list });

    expect(element.classList.contains('a')).to.be.true;
    expect(element.classList.contains('b')).to.be.true;

    list.fireSplice([], ['c']);
    expect(element.classList.contains('c')).to.be.true;

    list.fireSplice(['a'], []);
    expect(element.classList.contains('a')).to.be.false;

    // The returned remover detaches the splice listener.
    remover.remove();
    list.fireSplice([], ['d']);
    expect(element.classList.contains('d')).to.be.false;
  });
});
