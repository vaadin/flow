import { expect } from '@open-wc/testing';
import { Debouncer } from '../../main/frontend/internal/binding/Debouncer';

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

describe('Debouncer', () => {
  it('caches debouncers per element, identifier and timeout', () => {
    const el = document.createElement('div');
    const a = Debouncer.getOrCreate(el, 'click', 100);
    expect(Debouncer.getOrCreate(el, 'click', 100)).to.equal(a);
    expect(Debouncer.getOrCreate(el, 'click', 200)).to.not.equal(a);
    expect(Debouncer.getOrCreate(el, 'change', 100)).to.not.equal(a);

    const other = document.createElement('div');
    expect(Debouncer.getOrCreate(other, 'click', 100)).to.not.equal(a);
  });

  it('runs the buffered command and sends a trailing event after the timeout', async () => {
    const el = document.createElement('div');
    const debouncer = Debouncer.getOrCreate(el, 'trailing-test', 20);

    let sentPhase: string | null = null;
    let commandRan = false;
    const commands = new Map<string, () => void>([
      [
        'p',
        () => {
          commandRan = true;
        }
      ]
    ]);

    const immediate = debouncer.trigger(
      new Set(['trailing']),
      (phase) => {
        sentPhase = phase;
      },
      commands
    );

    expect(immediate).to.equal(false);
    expect(commandRan).to.equal(false);
    expect(sentPhase).to.equal(null);

    await delay(60);

    expect(commandRan).to.equal(true);
    expect(sentPhase).to.equal('trailing');
  });

  it('triggers a leading event immediately and debounces subsequent ones', async () => {
    const el = document.createElement('div');
    const debouncer = Debouncer.getOrCreate(el, 'leading-test', 20);

    const first = debouncer.trigger(new Set(['leading']), () => {}, new Map());
    expect(first).to.equal(true);

    const second = debouncer.trigger(new Set(['leading']), () => {}, new Map());
    expect(second).to.equal(false);

    // let the idle timer fire and unregister
    await delay(60);
  });

  describe('flushAll', () => {
    it('flushes a buffered trailing command and returns its send command', async () => {
      const el = document.createElement('div');
      // A long timeout so the idle timer never fires on its own during the test.
      const debouncer = Debouncer.getOrCreate(el, 'flush-buffered', 1000);

      let sentPhase: string | null = null;
      let commandRan = false;
      const sendCommand = (phase: string) => {
        sentPhase = phase;
      };
      const commands = new Map<string, () => void>([
        [
          'prop',
          () => {
            commandRan = true;
          }
        ]
      ]);

      // A trailing event buffers the command instead of running it now.
      const immediate = debouncer.trigger(new Set(['trailing']), sendCommand, commands);
      expect(immediate).to.equal(false);
      expect(commandRan).to.equal(false);

      const executed = Debouncer.flushAll();

      // The buffered command and its send command are flushed as a trailing event.
      expect(commandRan).to.equal(true);
      expect(sentPhase).to.equal('trailing');
      expect(executed).to.deep.equal([sendCommand]);

      // The buffered command is cleared, so a second flush does nothing.
      expect(Debouncer.flushAll()).to.deep.equal([]);

      // let the idle timer fire and unregister
      await delay(1100);
    });

    it('skips a leading-only debouncer that has no buffered command', () => {
      const el = document.createElement('div');
      const debouncer = Debouncer.getOrCreate(el, 'flush-leading', 1000);

      // A leading event fires immediately and buffers nothing; only an idle
      // timer remains registered.
      const immediate = debouncer.trigger(new Set(['leading']), () => expect.fail('should not send'), new Map());
      expect(immediate).to.equal(true);

      // Nothing buffered => flushAll returns no executed commands.
      expect(Debouncer.flushAll()).to.deep.equal([]);
    });
  });
});
