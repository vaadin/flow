// A frontend resource, imported only through @JsDefinitionImport. The build
// copies it with the frontend files of jars and puts it in a chunk that is
// loaded on the first call.
export function suffix(text) {
  return text + ' from add-on';
}
