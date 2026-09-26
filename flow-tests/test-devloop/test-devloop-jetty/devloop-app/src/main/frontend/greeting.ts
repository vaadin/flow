// Imported by ImportingLayout through @JsModule, so the view under test has a
// supertype that declares a frontend import - the shape every real Vaadin view
// has, and the one a fixture built on bare flow-html-components does not.
export function greeting(): string {
  return 'hello from the dev loop';
}
