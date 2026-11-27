// Imported only through @JsDefinitionImport, so the production build puts it in
// a chunk of its own that is loaded when a method of the definition is called.
window.jsDefinitionImportLoaded = true;

export default function describe(status) {
  return 'described ' + status;
}

export function shout(text) {
  return text.toUpperCase();
}
