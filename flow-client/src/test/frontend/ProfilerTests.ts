import { expect } from '@open-wc/testing';
import {
  clearEventsList,
  ensureLogger,
  ensureNoLogger,
  getGwtStatsEvents,
  getPerformanceTiming,
  hasHighPrecisionTime,
  round
} from '../../main/frontend/internal/Profiler';

describe('Profiler', () => {
  it('round rounds to the given number of decimal places', () => {
    expect(round(1.23456, 2)).to.equal(1.23);
    expect(round(1.235, 2)).to.equal(1.24);
    expect(round(10, 0)).to.equal(10);
    expect(round(1.5, 0)).to.equal(2);
  });

  it('hasHighPrecisionTime reflects performance.now availability', () => {
    // jsdom/Chromium provide performance.now.
    expect(hasHighPrecisionTime()).to.equal(typeof window.performance?.now === 'function');
  });

  it('getPerformanceTiming returns the named timing value or 0', () => {
    const timing = window.performance?.timing as unknown as Record<string, number> | undefined;
    if (timing && typeof timing.navigationStart === 'number') {
      expect(getPerformanceTiming('navigationStart')).to.equal(timing.navigationStart);
    }
    expect(getPerformanceTiming('no-such-timing-entry')).to.equal(0);
  });

  it('getGwtStatsEvents/clearEventsList read and reset the events list', () => {
    const win = window as unknown as { Vaadin?: { Flow?: { gwtStatsEvents?: unknown[] } } };
    const savedVaadin = win.Vaadin;
    try {
      win.Vaadin = { Flow: { gwtStatsEvents: [{ a: 1 }] } };
      expect(getGwtStatsEvents()).to.deep.equal([{ a: 1 }]);

      clearEventsList();
      expect(win.Vaadin.Flow?.gwtStatsEvents).to.deep.equal([]);

      win.Vaadin = { Flow: {} };
      expect(getGwtStatsEvents()).to.deep.equal([]);
    } finally {
      win.Vaadin = savedVaadin;
    }
  });

  it('ensureLogger installs a collecting __gwtStatsEvent, ensureNoLogger removes it', () => {
    const win = window as unknown as {
      Vaadin?: { Flow?: { gwtStatsEvents?: unknown[] } };
      __gwtStatsEvent?: (event?: unknown) => boolean;
    };
    const savedVaadin = win.Vaadin;
    const savedLogger = win.__gwtStatsEvent;
    try {
      win.Vaadin = { Flow: {} };
      win.__gwtStatsEvent = undefined;

      ensureLogger();
      expect(typeof win.__gwtStatsEvent).to.equal('function');
      win.__gwtStatsEvent!({ x: 1 });
      expect(win.Vaadin.Flow?.gwtStatsEvents).to.deep.equal([{ x: 1 }]);

      ensureNoLogger();
      expect(win.Vaadin.Flow?.gwtStatsEvents).to.equal(undefined);
      // The logger is neutralized (still a function, but a no-op returning true).
      expect(win.__gwtStatsEvent!()).to.be.true;
    } finally {
      win.Vaadin = savedVaadin;
      win.__gwtStatsEvent = savedLogger;
    }
  });
});
