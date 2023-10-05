//import Server
//import io.ktor.http.*
//import io.ktor.server.testing.*
//import junit.framework.TestCase.assertEquals
//import org.junit.jupiter.api.Test
//
//class ServerTest {
//
//    @Test
//    fun testHelloWorldEndpoint() {
//        withTestApplication({
//            configureRouting()
//        }) {
//            handleRequest(HttpMethod.Get, "/").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                assertEquals("Hello, World!", response.content)
//            }
//        }
//    }
//
//    private fun Application.configureRouting() {
//        routing {
//            get("/") {
//                call.respondText("Hello, World!", ContentType.Text.Plain)
//            }
//            // Добавьте другие маршруты вашего приложения здесь
//        }
//    }
//
//
////    @Test
////    fun testNumberRequest() {
////        withTestApplication({ Server.launch() }) {
////            handleRequest(HttpMethod.Get, "/number").apply {
////                assertEquals(HttpStatusCode.OK, response.status())
////                assertEquals("42", response.content)
////            }
////        }
////    }
//
////    @Test
////    fun testSumRequest() {
////        withTestApplication({ Server.launch() }) {
////            handleRequest(HttpMethod.Get, "/sum{10,20}").apply {
////                assertEquals(HttpStatusCode.OK, response.status())
////                assertEquals("Сумма чисел: 30", response.content)
////            }
////
////            handleRequest(HttpMethod.Get, "/sum{abc,def}").apply {
////                assertEquals(HttpStatusCode.BadRequest, response.status())
////                assertEquals("Invalid format", response.content)
////            }
////
////            handleRequest(HttpMethod.Get, "/sum{10}").apply {
////                assertEquals(HttpStatusCode.BadRequest, response.status())
////                assertEquals("Invalid format", response.content)
////            }
////        }
////    }
//}
