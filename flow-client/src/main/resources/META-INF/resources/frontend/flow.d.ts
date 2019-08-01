export interface FlowSettings {
    imports ?: Function;
}

export default class Flow {
    constructor(config ?: FlowSettings);
    start(): Promise<void>;
    navigate(context ?: Object, commands ?: Object): Promise<void>;
}
