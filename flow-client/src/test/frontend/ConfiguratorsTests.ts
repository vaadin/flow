import { expect } from '@open-wc/testing';
import { observe as observeLoadingIndicator } from '../../main/frontend/internal/communication/LoadingIndicatorConfigurator';
import { observe as observePoll } from '../../main/frontend/internal/communication/PollConfigurator';

// A MapProperty stand-in that captures its change listener and can fire it.
function fakeProperty() {
  let listener: ((event: any) => void) | null = null;
  return {
    addChangeListener(l: (event: any) => void) {
      listener = l;
      return { remove: () => {} };
    },
    fire(event: any) {
      listener?.(event);
    }
  };
}

describe('PollConfigurator', () => {
  it('configures the poller when the poll interval changes', () => {
    const property = fakeProperty();
    const node = { getMap: () => ({ getProperty: () => property }) };
    const intervals: number[] = [];
    observePoll(node, { setInterval: (i) => intervals.push(i) });

    property.fire({ getNewValue: () => 3000 });
    expect(intervals).to.deep.equal([3000]);
  });
});

describe('LoadingIndicatorConfigurator', () => {
  afterEach(() => {
    delete (window as { Vaadin?: unknown }).Vaadin;
  });

  it('applies delay and theme properties to the connection indicator', () => {
    const indicator: Record<string, unknown> = {};
    (window as { Vaadin?: unknown }).Vaadin = { connectionIndicator: indicator };

    const properties: Record<string, ReturnType<typeof fakeProperty>> = {};
    const node = {
      getMap: () => ({
        getProperty: (key: string) => {
          properties[key] ??= fakeProperty();
          return properties[key];
        }
      })
    };
    observeLoadingIndicator(node);

    // Fire each property's change with a source returning the given value.
    properties.first.fire({ getSource: () => ({ getValueOrDefault: () => 100 }) });
    properties.second.fire({ getSource: () => ({ getValueOrDefault: () => 200 }) });
    properties.third.fire({ getSource: () => ({ getValueOrDefault: () => 300 }) });
    properties.theme.fire({ getSource: () => ({ getValueOrDefault: () => false }) });

    expect(indicator).to.deep.equal({
      firstDelay: 100,
      secondDelay: 200,
      thirdDelay: 300,
      applyDefaultTheme: false
    });
  });
});
