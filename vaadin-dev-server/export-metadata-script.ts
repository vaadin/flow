/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { readdirSync, existsSync, rmSync, mkdirSync, writeFile } from 'fs';
import { join } from 'path';


async function main() {
    const metadataFolder = join('.', 'src', 'main', 'resources', 'META-INF', 'metadata');

    if (existsSync(metadataFolder)) {
        rmSync(metadataFolder, { recursive: true, force: true });
    }

    mkdirSync(metadataFolder);

    const fileNames = readdirSync(join('.', 'src', 'main', 'frontend', 'theme-editor', 'metadata', 'components'));
    for (const fileName of fileNames) {
        if (!fileName.startsWith('vaadin-')) {
            continue;
        }
        const tagName = fileName.split('.')[0];

        const metadata = (await import(`./src/main/frontend/theme-editor/metadata/components/${tagName}`)).default

        const metadataString = JSON.stringify(metadata.elements, null, 2);
        writeFile(join(metadataFolder, `${tagName}.json`), metadataString, (err) => {
            if(err !== null) console.log(err);
        });
    }
}

main().then(() => console.log('finish'))
