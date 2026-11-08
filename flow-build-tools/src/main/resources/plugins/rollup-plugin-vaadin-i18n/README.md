# rollup-plugin-vaadin-i18n

Internal Vite plugin, used in production builds only. It records which
translation keys of `@vaadin/hilla-react-i18n` each JS chunk uses, makes each
chunk load its own keys (`i18n.registerChunk`), and writes the result as
`i18n.json` for `TranslationFileRequestHandler`.

How translations for Hilla views work end to end is described in
[ARCHITECTURE.md](ARCHITECTURE.md).
