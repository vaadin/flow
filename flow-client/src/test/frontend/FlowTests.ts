const { suite, test } = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

import { Flow } from "../../main/resources/META-INF/resources/frontend/Flow";

suite("Flow", () => {

    test("should accept a configuration object", () => {
        const flow = new Flow({imports: () => {}});
        assert.isDefined(flow.config);
        assert.isDefined(flow.config.imports);
    });

    test("should have the start() method in the API", () => {
        return new Flow().start();
    });

    test("should have the navigate() method in the API", () => {
        return new Flow().navigate();
    });
});