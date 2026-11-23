// Uses Hilla i18n translations so that the Vaadin i18n build plugin rewrites
// the chunk this module ends up in, adding the registerChunk call for it.
import { key, translate } from '@vaadin/hilla-react-i18n';

window.i18nChunkTranslation = translate(key`i18n.chunk.test`);
