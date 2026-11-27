// A frontend file of a jar, imported only through @JsDefinitionImport, so the
// build copies it and puts it in a chunk that is loaded on the first call.
export function suffix(text) {
  return text + ' from add-on';
}
