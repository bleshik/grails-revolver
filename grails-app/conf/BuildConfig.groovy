grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    inherits("global") { }
    log "warn"
    repositories {
        mavenLocal()
        grailsCentral()
        mavenCentral()
    }

    plugins {
        build(":release:3.1.2",
              ":rest-client-builder:2.1.1") {
            export = false
        }
    }
}
