// A second module with translations in the same chunk, so that the build
// plugin has more than one registerChunk call to clean up.
import { key, translate } from '@vaadin/hilla-react-i18n';

window.i18nChunkExtraTranslation = translate(key`i18n.chunk.test.extra`);
