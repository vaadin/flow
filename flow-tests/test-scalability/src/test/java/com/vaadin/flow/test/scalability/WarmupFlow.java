package com.vaadin.flow.test.scalability;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class WarmupFlow extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8888").warmUp("http://localhost:8888/")
            .acceptHeader(
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.5").userAgentHeader(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:46.0) Gecko/20100101 Firefox/46.0");

    ScenarioBuilder scn = scenario("Warm up Jetty")
            .exec(http("Open hello world").get("/helloworld/"));

    {
        setUp(scn.injectOpen(atOnceUsers(1000))).protocols(httpProtocol);
    }

}
