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