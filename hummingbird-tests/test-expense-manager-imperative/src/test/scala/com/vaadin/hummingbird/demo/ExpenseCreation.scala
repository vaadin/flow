package scala.com.vaadin.hummingbird.demo

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class ExpenseCreation extends Simulation {

  val httpProtocol = http
    .baseURL("http://localhost:8888")
    .warmUp("http://localhost:8888/")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:46.0) Gecko/20100101 Firefox/46.0")

  val headers_1 = Map("Pragma" -> "no-cache")

  val headers_111 = Map(
    "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Pragma" -> "no-cache")

  val url = "/"
  val uidlUrl = url + "?v-r=uidl&v-uiId=${uiId}"

  val uidlHeaders = Map("Content-Type" -> "application/json; charset=UTF-8")

  val storeUiId =
    regex(""""v-uiId":\s(\d+),""")
      .saveAs("uiId")
  val storeSecurityKey =
    regex(""""Vaadin-Security-Key":\s"([^"]*)""")
      .saveAs("securityKey")

  val incrementIds = exec((session) => {
    session.setAll(
      "syncId" -> (session.get("syncId").as[Int] + 1),
      "clientId" -> (session.get("clientId").as[Int] + 1)
    )
  })

  val initialRequest = exec(http("Open index page")
      .get(url)
      .check(bodyString.saveAs("RESPONSE_DATA"))
      .check(storeUiId)
      .check(storeSecurityKey)
    )
    .doIf(session => !session.contains("uiId")) {
      exec(session => {
        println("init failed with response:")
        println(session("RESPONSE_DATA").as[String])
        session
      }
      )
    }
    .exec(session => session.set("clientId", 0))
    .exec(session => session.set("syncId", 0))
    .pause(261 milliseconds)

  val loadPage =
    exec(http("Request Items 0-50")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0112_request.txt"))
    )
    .exec(incrementIds)
    .pause(4)
  // load

  val openNewExpense =
    exec(http("pSync properties")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0114_request.txt"))
    )
    .exec(incrementIds)
    .exec(http("Click new expense")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0115_request.txt"))
      .check(regex("""add":\["cancel"""))
    )
    .exec(incrementIds)
    .pause(7)


  // Open new Expense

  val cancelNewExpense =
    exec(http("pSync properties")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0116_request.txt"))
    )
    .exec(incrementIds)

    .pause(419 milliseconds)
    .exec(http("Click cancel in expense window")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0118_request.txt"))
    )
    .exec(incrementIds)
    .pause(933 milliseconds)
  // cancel

  val scn = scenario("RecordedSimulation").exec(
    initialRequest).exitHereIfFailed.exec(loadPage).exitHereIfFailed.exec(openNewExpense).exitHereIfFailed.exec(cancelNewExpense)

  setUp(scn.inject(rampUsers(1000) over (120 seconds))).protocols(httpProtocol)

  /* To get response data for exec post add to Exec after body add:

  .check( bodyString.saveAs( "RESPONSE_DATA" ) )
    )
      .exec( session => {
        println( "0016:" )
        println( session( "RESPONSE_DATA" ).as[String] )
        session
      }
    */
}
