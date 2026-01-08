declare global {
  interface DevToolsConfiguration {
    readonly enable: boolean;
    readonly url: string;
    readonly backend: 'SPRING_BOOT_DEVTOOLS' | 'QUARKUS_DEVTOOLS';
    readonly liveReloadPort: number;
    readonly token: string;
  }

  interface Vaadin {
    readonly devToolsPlugins: readonly unknown[];
    readonly devToolsConf?: DevToolsConfiguration;
  }
}

const part: Pick<Vaadin, 'devToolsPlugins' | 'devToolsConf'> = {
  devToolsPlugins: []
};

export default part;
