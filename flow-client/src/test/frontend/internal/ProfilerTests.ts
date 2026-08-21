import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import {
  enter,
  getRelativeTimeMillis,
  getRelativeTimeString,
  initialize,
  isEnabled,
  leave,
  logBootstrapTimings,
  logTimings,
  type Node,
  reset,
  setEnabled,
  setProfilerResultConsumer
} from '../../../main/frontend/internal/Profiler';

type Win = {
  Vaadin?: { Flow?: { gwtStatsEvents?: unknown[] } };
  __gwtStatsEvent?: (event?: unknown) => boolean;
};
const win = window as unknown as Win;

describe('Profiler', () => {
  // The result consumer is a module-level singleton that may only be set once
  // (setProfilerResultConsumer throws otherwise), so it is installed once and
  // captures into holders that each test resets.
  let lastProfilerData: { rootNode: Node; totals: Node[] } | undefined;
  let lastBootstrapData: Map<string, number> | undefined;

  before(() => {
    setProfilerResultConsumer({
      addProfilerData(rootNode, totals) {
        lastProfilerData = { rootNode, totals };
      },
      addBootstrapData(timings) {
        lastBootstrapData = timings;
      }
    });
  });

  let savedVaadin: Win['Vaadin'];
  let savedLogger: Win['__gwtStatsEvent'];

  beforeEach(() => {
    savedVaadin = win.Vaadin;
    savedLogger = win.__gwtStatsEvent;
    win.Vaadin = { Flow: {} };
    win.__gwtStatsEvent = undefined;
    lastProfilerData = undefined;
    lastBootstrapData = undefined;
  });

  afterEach(() => {
    setEnabled(false);
    win.Vaadin = savedVaadin;
    win.__gwtStatsEvent = savedLogger;
  });

  it('isEnabled reflects setEnabled', () => {
    expect(isEnabled()).to.be.false;
    setEnabled(true);
    expect(isEnabled()).to.be.true;
  });

  it('getRelativeTimeMillis/getRelativeTimeString use performance.now and round to 3 decimals', () => {
    const nowStub = sinon.stub(window.performance, 'now').returns(5.6789);
    try {
      initialize();
      expect(getRelativeTimeMillis()).to.equal(5.6789);
      expect(getRelativeTimeString(0)).to.equal('5.679');
      expect(getRelativeTimeString(5)).to.equal('0.679');
    } finally {
      nowStub.restore();
    }
  });

  it('enter/leave forward begin/end events through the installed logger', () => {
    setEnabled(true);
    initialize();

    enter('bootstrap');
    leave('bootstrap');

    const events = win.Vaadin!.Flow!.gwtStatsEvents as Array<Record<string, unknown>>;
    expect(events).to.have.length(2);
    expect(events[0]).to.include({ evtGroup: 'VaadinProfiler', subSystem: 'bootstrap', type: 'begin' });
    expect(events[1]).to.include({ evtGroup: 'VaadinProfiler', subSystem: 'bootstrap', type: 'end' });
    expect(events[0].millis).to.be.a('number');
    expect(events[0].relativeMillis).to.be.a('number');
  });

  it('does nothing on enter/leave when profiling is disabled', () => {
    setEnabled(false);
    initialize();

    enter('x');
    leave('x');

    expect(win.Vaadin!.Flow!.gwtStatsEvents).to.be.undefined;
  });

  it('initialize removes and neutralizes a pre-existing page logger when disabled', () => {
    win.Vaadin = { Flow: { gwtStatsEvents: [{ a: 1 }] } };
    let pushed = false;
    win.__gwtStatsEvent = () => {
      pushed = true;
      return true;
    };

    setEnabled(false);
    initialize();

    expect(win.Vaadin.Flow!.gwtStatsEvents).to.be.undefined;
    // Neutralized: still a function returning true, but no longer collecting.
    expect(win.__gwtStatsEvent!({ b: 2 })).to.be.true;
    expect(pushed).to.be.false;
  });

  it('reset clears the collected events', () => {
    setEnabled(true);
    initialize();
    enter('a');
    leave('a');
    expect((win.Vaadin!.Flow!.gwtStatsEvents as unknown[]).length).to.be.greaterThan(0);

    reset();

    expect(win.Vaadin!.Flow!.gwtStatsEvents).to.deep.equal([]);
  });

  it('logTimings builds a node tree and feeds the consumer', () => {
    setEnabled(true);
    initialize();
    // Synthesize a begin/end pair for a single "foo" block spanning 30 ms.
    win.Vaadin!.Flow!.gwtStatsEvents = [
      { evtGroup: 'VaadinProfiler', subSystem: 'foo', type: 'begin', millis: 100 },
      { evtGroup: 'VaadinProfiler', subSystem: 'foo', type: 'end', millis: 130 }
    ];

    logTimings();

    expect(lastProfilerData).to.not.be.undefined;
    const foo = lastProfilerData!.totals.find((node) => node.getName() === 'foo');
    expect(foo).to.not.be.undefined;
    expect(foo!.getCount()).to.equal(1);
    expect(foo!.getTimeSpent()).to.equal(30);
  });

  it('logTimings does not feed the consumer when profiling is disabled', () => {
    setEnabled(false);
    win.Vaadin!.Flow!.gwtStatsEvents = [
      { evtGroup: 'VaadinProfiler', subSystem: 'foo', type: 'begin', millis: 100 },
      { evtGroup: 'VaadinProfiler', subSystem: 'foo', type: 'end', millis: 130 }
    ];

    logTimings();

    expect(lastProfilerData).to.be.undefined;
  });

  it('logBootstrapTimings reads performance.timing without throwing', () => {
    setEnabled(true);

    logBootstrapTimings();

    // performance.timing availability varies by environment; when entries are
    // present the consumer receives a timings map. Either way the call must
    // exercise the reader without throwing.
    if (lastBootstrapData !== undefined) {
      expect(lastBootstrapData).to.be.instanceOf(Map);
    }
  });
});
