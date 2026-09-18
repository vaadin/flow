declare module 'sw-precache' {

  export interface SWConfig {
    cacheId?: string;
    directoryIndex?: string;
    dynamicUrlToDependencies?: {
      [property: string]: string[]
    };
    handleFetch?: boolean;
    ignoreUrlParametersMatching?: RegExp[];
    importScripts?: string[];
    logger?: Function;
    maximumFileSizeToCacheInBytes?: number;
    navigateFallback?: string;
    navigateFallbackWhitelist?: RegExp[];
    replacePrefix?: string;
    runtimeCaching?: {
      urlPattern: RegExp;
      handler: string;
      options?: {
        cache: {
          maxEntries: number;
          name: string;
        };
      };
    }[];
    staticFileGlobs?: string[];
    stripPrefix?: string;
    templateFilePath?: string;
    verbose?: boolean;
  }

  export function generate(options: SWConfig, callback: (err?: Error, fileContents?: string) => void): void;
  export function write(filepath: string, options: SWConfig, callback: (err?: Error) => void): void;
}