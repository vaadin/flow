import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

import { writeFile, existsSync, rmSync, mkdirSync } from 'fs';
import buttonMetadata from '../../vaadin-dev-server/frontend/theme-editor/metadata/components/vaadin-button'

const customConfig: UserConfigFn = (env) => ({
    // Here you can add custom Vite parameters
    // https://vitejs.dev/config/
});

export default overrideVaadinConfig(customConfig);

const run = () => {
    const metadataFolder = './metadata'

    if (existsSync(metadataFolder)) {
        rmSync(metadataFolder, { recursive: true, force: true });
    }
	
	mkdirSync(metadataFolder);
	
    const buttonMetadataString = JSON.stringify(buttonMetadata.elements, null, 2);
    writeFile(`${metadataFolder}/vaadin-button.txt`, buttonMetadataString, (err) => {
        if(err !== null) console.log(err);
    });
};
run();
