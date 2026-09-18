declare module 'selenium-standalone' {
  import * as child_process from 'child_process';
  export interface DriverConfig {
    version: string;
    arch: string;
    baseURL: string;
  }
  export interface InstallOpts {
    /** Selenium version to install. */
    version: string;

    /** Used to find the server having the selenium or drivers files. */
    baseURL?: string;

    /**
     * Sets the base directory used to store the selenium standalone .jar and
     * drivers. Defaults to current working directory + .selenium/
     */
    basePath?: string;

    /** Drivers to download and install along with selenium standalone server */
    drivers?: {
      [browserName: string]: DriverConfig;
      // chrome?: DriverConfig;
      // ie?: DriverConfig;
      // ... complete list?
    };

    /**
     * Will be called if provided with some debugging information about the
     * installation process.
     */
    logger?: (message: any) => void;

    progressCb?: (totalLength: number, progressLength: number, chunkLength: number) => void;

  }
  export function install(opts: InstallOpts, cb: (error?: any) => void): void;

  export interface StartOpts {
    drivers?: {
      [browserName: string]: DriverConfig;
      // chrome?: DriverConfig;
      // ie?: DriverConfig;
      // ... complete list?
    };

    /**
     * Sets the base directory used to store the selenium standalone .jar and
     * drivers. Defaults to current working directory + .selenium/
     */
    basePath?: string;

    /** Spawn options for the selenium server. */
    spawnOptions?: child_process.SpawnOptions;

    /**
     * Arguments for the selenium server, passed directly to
     * child_process.spawn.
     */
    seleniumArgs?: string[];
    
    /** seleniumJavaArgs */
    javaArgs?: string[];

    /** set the javaPath manually, otherwise we use `which` */
    javaPath?: string;

    /**
     * Will be called if provided as soon as the selenium child process was
     * spawned. It may be interesting if you want to do some more debug.
     */
    spawnCb?: Function;

    /**
     * Called when the server is running and listening, child is the
     * ChildProcess instance created.
     */
    cb?: (err: any, child: child_process.ChildProcess) => void;
  }
  export function start(opts: StartOpts, cb: (error?: any) => void): void;
}
