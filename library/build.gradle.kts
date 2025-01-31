plugins {
  id("com.android.library")
//    id("maven-publish")
//  id("com.vanniktech.maven.publish")
}

android {
    compileSdk = Config.SdkVersions.compile

    defaultConfig {
        minSdk = Config.SdkVersions.min
        targetSdk = Config.SdkVersions.target

        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {    
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        // Common lint options across all modules
        disable += mutableSetOf(
            "IconExpectedSize",
            "InvalidPackage", // Firestore uses GRPC which makes lint mad
            "NewerVersionAvailable", "GradleDependency", // For reproducible builds
            "SelectableText", "SyntheticAccessor" // We almost never care about this
        )

        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = false // Override

        baseline = file("$rootDir/library/quality/lint-baseline.xml")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }

        multipleVariants {
            withSourcesJar()
            withJavadocJar()
            allVariants()
        }
    }
}

dependencies {
    api(project(":auth"))
    api(project(":database"))
    api(project(":firestore"))
    api(project(":storage"))
}

tasks.register("prepareArtifacts") {
    dependsOn("assembleRelease")
    dependsOn(*Config.submodules.map {
        ":$it:assembleRelease"
    }.toTypedArray())
}

//afterEvaluate {
//    publishing {
//        publications {
//            register<MavenPublication>("release") {
//                groupId = "com.github.yosuke65"
//                artifactId = "firebase-ui-slo"
//                version = "1.0.4"
//
//                afterEvaluate {
//                    from(components["release"])
//                }
//            }
//        }
//    }
//}
