import { expect } from '@open-wc/testing';
import {
  getOrCreateExpression,
  resolveDebounces,
  resolveFilters
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

describe('SimpleElementBindingStrategy event-data helpers', () => {
  it('getOrCreateExpression compiles and caches an (event, element) function', () => {
    const expr = getOrCreateExpression('event.detail + element.tabIndex');
    expect(expr({ detail: 5 } as any, { tabIndex: 2 } as any)).to.equal(7);
    // Same string => same cached function instance.
    expect(getOrCreateExpression('event.detail + element.tabIndex')).to.equal(expr);
  });

  it('resolveDebounces treats a zero timeout as eager', () => {
    const element = document.createElement('div');
    const eager = resolveDebounces(element, 'on-click:x', [[0]], () => {}, new Map());
    expect(eager).to.be.true;
  });

  it('resolveDebounces fires a leading debounce immediately but buffers a trailing one', () => {
    const element = document.createElement('div');
    // A leading phase with a fresh debouncer triggers now.
    const eager = resolveDebounces(element, 'on-input:y', [[50, 'leading']], () => {}, new Map());
    expect(eager).to.be.true;
    // A trailing debounce buffers instead, so it is not eager.
    const element2 = document.createElement('div');
    const trailing = resolveDebounces(element2, 'on-input:y', [[50, 'trailing']], () => {}, new Map());
    expect(trailing).to.be.false;
  });

  it('resolveFilters returns true when there are no active filters', () => {
    const element = document.createElement('div');
    // All settings falsy => treated as no filters => send.
    const result = resolveFilters(element, 'click', { a: false }, null, () => {}, new Map());
    expect(result).to.be.true;
  });

  it('resolveFilters matches a boolean filter only when the event data is truthy', () => {
    const element = document.createElement('div');
    const matched = resolveFilters(element, 'click', { needsCtrl: true }, { needsCtrl: true }, () => {}, new Map());
    expect(matched).to.be.true;

    const notMatched = resolveFilters(element, 'click', { needsCtrl: true }, { needsCtrl: false }, () => {}, new Map());
    expect(notMatched).to.be.false;
  });

  it('resolveFilters resolves a debounce filter via resolveDebounces', () => {
    const element = document.createElement('div');
    // The filter is present in eventData and its settings are a debounce list
    // with an eager (zero-timeout) entry => sent now.
    const result = resolveFilters(element, 'input', { typed: [[0]] }, { typed: true }, () => {}, new Map());
    expect(result).to.be.true;
  });
});
