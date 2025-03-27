allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://javacv.scijava.org/")
        }
        maven {
            url = uri("https://packagecloud.io/bytedeco/javacpp/maven2/")
        }
    }
}

val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
