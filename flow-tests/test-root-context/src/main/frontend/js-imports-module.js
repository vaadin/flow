// Values exported here are reachable from a server-sent JS expression only
// through @JsModule(imports = ...); a plain expression is evaluated in the
// global scope and cannot import them.
export function setText(element, text) {
  element.textContent = text;
}

export const marker = 'from-js-imports-module';

export default function markDefault(element) {
  element.setAttribute('data-default', 'yes');
}
