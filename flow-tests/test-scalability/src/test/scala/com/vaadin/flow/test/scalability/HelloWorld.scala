/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.test.scalability

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class HelloWorld extends Simulation {

  val httpProtocol = http
    .baseURL("http://localhost:8888")
    .warmUp("http://localhost:8888/")
    .inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.(t|o)tf""", """.*\.png"""), WhiteList())
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:46.0) Gecko/20100101 Firefox/46.0")

  val uidlHeaders = Map("Content-Type" -> "application/json; charset=UTF-8")

  val url = "/helloworld/"
  val uidlUrl = url + "?v-r=uidl&v-uiId=${uiId}"

  object PageObject {
    val storeUiId =
      regex(""""v-uiId":\s(\d+),""")
        .saveAs("uiId")
    val storeSecurityKey =
      regex(""""Vaadin-Security-Key":\s"([^"]*)""")
        .saveAs("securityKey")
    val hello = regex(""""type":"put","key":"text","feat":7,"value":"Hello!"""")

    val bootstrap = exec(http("Open index page")
      .get(url)
      .check(storeUiId)
      .check(storeSecurityKey))
      .exec(session => session.set("clientId", 0))
      .exec(session => session.set("syncId", 0))

    val clickButton = exec(http("Click button")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("clickButton.json"))
      .check(hello))
      .exec(session => session.set("syncId", session.get("syncId").as[Int] + 1))
      .exec(session => session.set("clientId", session.get("clientId").as[Int] + 1))
  }

  var scn = scenario("Click hello three times").exec(
    PageObject.bootstrap,
    PageObject.clickButton,
    pause(2),
    PageObject.clickButton,
    pause(2),
    PageObject.clickButton);

    setUp(scn.inject(rampUsers(1000) over (5 seconds))).protocols(httpProtocol)
}
