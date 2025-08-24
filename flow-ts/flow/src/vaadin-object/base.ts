declare global {
  interface AppConfig {
    readonly productionMode: boolean;
    readonly appId: string;
    readonly uidl: unknown;
  }

  interface AppInitPayload {
    readonly appConfig: AppConfig;
    readonly pushScript?: string;
  }

  interface VaadinTypeScript {
    readonly initial?: AppInitPayload;
  }

  interface Vaadin {
    TypeScript: VaadinTypeScript;
  }
}

const part: Pick<Vaadin, 'TypeScript'> = {
  TypeScript: {}
};

export default part;
