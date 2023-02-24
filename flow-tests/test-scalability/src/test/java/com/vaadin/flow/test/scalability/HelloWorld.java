package com.vaadin.flow.test.scalability;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.AllowList;
import static io.gatling.javaapi.core.CoreDsl.DenyList;
import static io.gatling.javaapi.core.CoreDsl.ElFileBody;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.regex;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class HelloWorld extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8888").warmUp("http://localhost:8888/")
            .inferHtmlResources(AllowList(),
                    DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg",
                            ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.(t|o)tf",
                            ".*\\.png"))
            .acceptHeader(
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.5").userAgentHeader(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");

    private Map uidlHeaders = Collections.singletonMap("Content-Type",
            "application/json; charset=UTF-8");

    private String uidlUrl = "http://localhost:8888/?v-r=uidl&v-uiId=${uiId}";

    private String initUrl = "http://localhost:8888?v-r=init&location=helloworld";

    private class PageObject {
        CheckBuilder.Final storeUiId = jsonPath("$.appConfig.v-uiId")
                .saveAs("uiId");
        CheckBuilder.Final storeSecurityKey = jsonPath(
                "$.appConfig.uidl.Vaadin-Security-Key").saveAs("securityKey");

        CheckBuilder.Final appId = jsonPath("$.appConfig.appId")
                .saveAs("appId");
        CheckBuilder.CaptureGroupCheckBuilder hello = regex(
                "\"type\":\"put\",\"key\":\"text\",\"feat\":7,\"value\":\"Hello!\"");

        ChainBuilder bootstrap = exec(http("Open index page").get(initUrl)
                .check(storeUiId).check(storeSecurityKey).check(appId))
                .exec(http("Connect client").post(uidlUrl).headers(uidlHeaders)
                        .body(ElFileBody("bodies/connectClient.json")).check())
                .exec(session -> session.set("clientId", 1))
                .exec(session -> session.set("syncId", 1));

        ChainBuilder clickButton = exec(http("Click button").post(uidlUrl)
                .headers(uidlHeaders)
                .body(ElFileBody("bodies/clickButton.json")).check(hello))
                .exec(session -> session.set("syncId",
                        session.getInt("syncId") + 1))
                .exec(session -> session.set("clientId",
                        session.getInt("clientId") + 1));
    }

    {

        PageObject pageObject = new PageObject();

        ScenarioBuilder scn = scenario("Click hello three times").exec(
                pageObject.bootstrap, pageObject.clickButton, pause(2),
                pageObject.clickButton, pause(2), pageObject.clickButton);

        setUp(scn.injectOpen(rampUsers(100).during(Duration.ofSeconds(5))))
                .protocols(httpProtocol);
    }
}
