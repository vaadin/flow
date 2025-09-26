declare global {
  interface Vaadin {}

  const Vaadin: Vaadin;

  interface Window {
    Vaadin: Vaadin;
  }
}

export type WritableArray<T> = T extends ReadonlyArray<infer U> ? U[] : T;

export type MaybePromise<T> = T | Promise<T>;
