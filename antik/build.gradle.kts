plugins {
    java
    `java-library`
    alias(libs.plugins.shadow)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveFileName.set("RePairip.jar")
    manifest {
        attributes["Main-Class"] = "com.antik.Main"
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

dependencies {
    api(libs.dexlib2)
    api(libs.reandroid.arsclib)
    implementation(libs.guava)
}
