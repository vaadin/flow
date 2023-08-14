import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

import { writeFileSync } from 'fs';
import buttonMetadata from '../../vaadin-dev-server/frontend/theme-editor/metadata/components/vaadin-button'

const customConfig: UserConfigFn = (env) => ({
    // Here you can add custom Vite parameters
    // https://vitejs.dev/config/
});

export default overrideVaadinConfig(customConfig);

const run = (cmd) => {
    const buttonMetadataString = JSON.stringify(buttonMetadata.elements, null, 2);
    writeFileSync('./target/test-classes/vaadin-button.txt', buttonMetadataString, {flag: 'w'});
};
run('parseClientRoutes');
