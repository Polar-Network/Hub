plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "net.polar"
version = "1.0"

//val folderAboveCurrent = File("$rootDir").parentFile!!
//val testServer = File("${folderAboveCurrent.path}/TestingServers/hub").apply {
//    if (!exists()) mkdirs()
//}
val testServer = File("$rootDir/test-server").apply {
    if (!exists()) mkdirs()
}


repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {

    implementation("com.github.Polar-Network:Polaroid:-SNAPSHOT")

}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("hub.jar")
        manifest {
            attributes(
                "Main-Class" to "net.polar.Hub",
            )
        }
        val file = this.archiveFile.get().asFile
        doLast {
            copy {
                from(file)
                into(testServer)
            }
        }
    }

}