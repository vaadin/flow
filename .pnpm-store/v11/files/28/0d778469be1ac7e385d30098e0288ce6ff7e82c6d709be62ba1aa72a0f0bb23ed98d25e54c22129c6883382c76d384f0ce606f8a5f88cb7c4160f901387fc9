declare module 'command-line-args' {
  module commandLineArgs {
    interface ArgDescriptor {
      name: string;
      alias?: string;
      description?: string;
      defaultValue?: any;
      type?: Object;
      multiple?: boolean;
      defaultOption?: boolean;
      group?: string;
    }
  }

  /**
   * @param descriptors An array of objects that describe the arguments that
   *     we want to parse.
   * @param args Optional arguments to parse. If not given, process.argv is
   *     used.
   */
  function commandLineArgs(
      descriptors: commandLineArgs.ArgDescriptor[], args?: string[]): any;

  export = commandLineArgs;
}
