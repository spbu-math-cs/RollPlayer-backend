import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*

fun main() {
    println("Hello World!")
    val config = HoconApplicationConfig(ConfigFactory.load())
    Server.launch(config)
}
