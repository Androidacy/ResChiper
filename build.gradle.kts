plugins {
    id("java")
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "com.androidacy.reschiper"
version = "0.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(gradleApi())
    implementation("org.jetbrains:annotations:26.0.1")
    implementation("com.android.tools.build:gradle:9.0.0")
    implementation("com.android.tools.build:bundletool:1.18.3")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("io.grpc:grpc-protobuf:1.78.0")
    implementation("com.android.tools.build:aapt2-proto:9.0.0-14304508")
    implementation("commons-codec:commons-codec:1.17.1")
    implementation("commons-io:commons-io:2.18.0")
    implementation("org.dom4j:dom4j:2.1.4")
    implementation("com.google.auto.value:auto-value-annotations:1.11.0")
    annotationProcessor("com.google.auto.value:auto-value:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("resChiper") {
            id = "com.androidacy.reschiper"
            implementationClass = "com.androidacy.reschiper.ResChiperPlugin"
            displayName = "ResChiper"
            description = "AAB Resource Obfuscation Tool"
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    if (project.hasProperty("signingInMemoryKey")) {
        signAllPublications()
    }

    coordinates("com.androidacy.reschiper", "plugin", version.toString())

    pom {
        name.set("ResChiper")
        description.set("A tool for obfuscating Android AAB resources")
        url.set("https://github.com/nickkelly/ResChiper")
        inceptionYear.set("2023")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("nickkelly")
                name.set("Nick Kelly")
                email.set("nickkelly@androidacy.com")
                organization.set("Androidacy")
                organizationUrl.set("https://androidacy.com")
            }
            developer {
                id.set("goldfish07")
                name.set("Ayush Bisht")
                email.set("ayushbisht5663@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/nickkelly/ResChiper.git")
            developerConnection.set("scm:git:ssh://github.com/nickkelly/ResChiper.git")
            url.set("https://github.com/nickkelly/ResChiper")
        }
    }
}
