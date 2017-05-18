package scala.com.vaadin.flow.demo

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

  //  Runtime component node ids collected from responses
  val storeGridNodeId = regex(""""node":\s?(\d+),[\s]*"type":\s?"put",[\s]*"key":\s?"tag",[\s]*"feat":\s?0,[\s]*"value":\s?"vaadin-grid"""").saveAs("gridNode")
  val addButton = regex(""""node":\s?(\d+),[\s]*"type":\s?"put",[\s]*"key":\s?"id",[\s]*"feat":\s?3,[\s]*"value":\s?"add-button"""").saveAs("addButton")
  val cancelButton = regex(""""node":(\d+),[\s]*"type":"splice",[\s]*"feat":11,[\s]*"index":0,[\s]*"add":\["cancel-button"\]""").saveAs("cancelButton")

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
      .check(storeGridNodeId)
      .check(addButton)
    )
    .doIf(session => !session.contains("uiId")) {
      exec(session => {
        println("init failed with response:")
        println(session("RESPONSE_DATA").as[String])
        session
      })
    }
    .exec(session => session.set("clientId", 0))
    .exec(session => session.set("syncId", 0))
    .pause(261 milliseconds)

  val loadPage =
    exec(http("Request Items 0-50")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0112_request.txt"))
      .check(regex(""""id":0,"""))
    )
    .exec(incrementIds)
    .pause(4)
  // load

  val openNewExpense =
    exec(http("mSync properties")
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
      .check(cancelButton)
    )
    .exec(incrementIds)
    .pause(7)


  // Open new Expense

  val cancelNewExpense =
    exec(http("mSync properties")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0116_request.txt"))
    )
    .exec(incrementIds)

    .pause(419 milliseconds)
    .exec(http("Click cancel in expense window")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0117_request.txt"))
      .check(storeGridNodeId)
//        .check(regex(""""node":105,"type":"attach"""))
    )
    .exec(incrementIds)

    .pause(419 milliseconds)

  val loadListContent =
    exec(http("Load items to grid")
      .post(uidlUrl)
      .headers(uidlHeaders)
      .body(ElFileBody("RecordedSimulation_0118_request.txt"))
        .check(regex(""""id":0,"""))
    )
    .exec(incrementIds)
    .pause(933 milliseconds)
  // cancel

  val scn = scenario("RecordedSimulation")
    .exec(initialRequest).exitHereIfFailed
    .exec(loadPage).exitHereIfFailed
    .exec(openNewExpense).exitHereIfFailed
    .exec(cancelNewExpense).exitHereIfFailed
    .exec(loadListContent)

  setUp(scn.inject(rampUsers(2000) over (120 seconds))).protocols(httpProtocol)

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
