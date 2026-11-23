// Minimal stand-in for the Hilla i18n runtime. The Vaadin i18n build plugin
// looks for imports of this package and adds an i18n.registerChunk call to
// every chunk that uses translations, so the tests need the package to exist
// but not to load any actual translations.
export const i18n = {
  async registerChunk(chunkName) {
    window.registeredI18nChunks = [...(window.registeredI18nChunks ?? []), chunkName];
  }
};

export function key(strings) {
  return strings[0];
}

export function translate(translationKey) {
  return `translated:${translationKey}`;
}
