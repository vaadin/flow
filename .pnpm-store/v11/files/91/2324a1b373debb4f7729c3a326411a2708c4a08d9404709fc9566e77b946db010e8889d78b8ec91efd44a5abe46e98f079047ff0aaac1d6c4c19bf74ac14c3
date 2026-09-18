declare module 'command-line-usage' {
  function commandLineUsage(args: commandLineUsage.Section[]): any;

  module commandLineUsage {
    interface Section {
      header?: string;
      title?: string;
      content?: string;
      optionList?: ArgDescriptor[];
    }
    interface ArgDescriptor {
      name: string;
      alias?: string;
      description?: string;
      defaultValue?: any;
      type?: Object;
      multiple?: boolean;
      defaultOption?: boolean;
    }
  }

  export = commandLineUsage;
}
