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
});
