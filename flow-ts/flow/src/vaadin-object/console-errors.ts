import type { WritableArray } from '../types.ts';

declare global {
  interface Vaadin {
    ConsoleErrors: readonly unknown[];
  }
}

const ConsoleErrors: WritableArray<Vaadin['ConsoleErrors']> = [];

const part: Pick<Vaadin, 'ConsoleErrors'> = {
  ConsoleErrors
};

export default part;

const browserConsoleError = console.error.bind(console);

console.error = (...args: Parameters<typeof console.error>) => {
  browserConsoleError(...args);
  ConsoleErrors.push(args);
};

window.onerror = (message, source, lineno, colno) => {
  const location = `${source}:${lineno}:${colno}`;
  ConsoleErrors.push([message, `(${location})`]);
};

addEventListener('unhandledrejection', (e) => {
  ConsoleErrors.push(e.reason);
});
