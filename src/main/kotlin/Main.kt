import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
//    val applicationConfig = HoconApplicationConfig(ConfigFactory.load())
//    val port = applicationConfig.property("ktor.deployment.port").getString().toInt()
//    val host = applicationConfig.property("ktor.deployment.host").getString()
//
//    val application = embeddedServer(Netty, port = port, host = host) {
//        module(applicationConfig)
//    }
//
//    application.start(wait = true)
    val applicationConfig = HoconApplicationConfig(ConfigFactory.load())
    val application = embeddedServer(Netty, port = 8083, host = "0.0.0.0") {
        module(applicationConfig)
    }

    application.start(wait = true)
}
