
class Flow {
    constructor(config) {
        window.console.log("Flow.new", config);
    }
    async start() {
        window.console.log("Flow.start");
    }
    async navigate(context, commands) {
        window.console.log("Flow.navigate", this, context, commands);
    }
}

export default Flow;