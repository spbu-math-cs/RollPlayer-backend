<<<<<<< HEAD
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
=======
import db.*

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    DBOperator.connectOrCreate()

    DBOperator.removeNonExistingMaps()
    DBOperator.createNewMap("test_map", "TestMap")
    DBOperator.createNewMap("test_map2", "TestMap")
    DBOperator.getAllMapInfos()
        .forEach { print(it.id); println(it.pathToJson) }

    DBOperator.addUser(UserInfo("Vasia", "vasia1234567"))
    DBOperator.addUser(UserInfo("Petya", "uewjn"))
    DBOperator.addUser(UserInfo("Clara", "uewjn"))

    DBOperator.getAllUsers()
        .forEach { println(it.login) }
}
>>>>>>> eda4491e77cfc0b840c7bb77e18cce92400814dc
