import { ComponentReference } from '../../component-util';
import { ComponentMetadata } from './model';
import { createGenericMetadata } from './components/generic';

type MetadataMap = { [key: string]: ComponentMetadata };

type ModuleLoader = (modulePath: string) => Promise<any>;

const defaultModuleLoader: ModuleLoader = (tagName: string) => {
  // Module path needs to be part of the import statement for Vite bundling to work
  return import(`./components/${tagName}.ts`);
};

export class MetadataRegistry {
  private metadata: MetadataMap = {};

  constructor(private loader: ModuleLoader = defaultModuleLoader) {}

  async getMetadata(component: ComponentReference): Promise<ComponentMetadata | null> {
    // Ignore if there is no element
    const tagName = component.element?.localName;
    if (!tagName) {
      return null;
    }

    // For non-Vaadin elements, return generic metadata
    if (!tagName.startsWith('vaadin-')) {
      return createGenericMetadata(tagName);
    }

    // Check for existing metadata
    let metadata = this.metadata[tagName];
    if (metadata) {
      return metadata;
    }

    // Try load metadata for component
    try {
      const module = await this.loader(tagName);
      metadata = module.default;
      this.metadata[tagName] = metadata;
    } catch (error) {
      console.warn(`Failed to load metadata for component: ${tagName}`);
    }

    return metadata || null;
  }
}

export const metadataRegistry = new MetadataRegistry();
