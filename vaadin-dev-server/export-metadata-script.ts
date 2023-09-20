import { readdirSync, existsSync, rmSync, mkdirSync, writeFile } from 'fs';
import { join } from 'path';


async function main() {
    const metadataFolder = join('.', 'src', 'main', 'resources', 'META-INF', 'metadata');

    if (existsSync(metadataFolder)) {
        rmSync(metadataFolder, { recursive: true, force: true });
    }

    mkdirSync(metadataFolder);

    const fileNames = readdirSync(join('.', 'frontend', 'theme-editor', 'metadata', 'components'));
    for (const fileName of fileNames) {
        if (!fileName.startsWith('vaadin-')) {
            continue;
        }
        const tagName = fileName.split('.')[0];

        const metadata = (await import(`./frontend/theme-editor/metadata/components/${tagName}`)).default

        const metadataString = JSON.stringify(metadata.elements, null, 2);
        writeFile(join(metadataFolder, `${tagName}.json`), metadataString, (err) => {
            if(err !== null) console.log(err);
        });
    }
}

main().then(() => console.log('finish'))
