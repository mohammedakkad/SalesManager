rootProject.name = "seller-pdf-verifier"
dependencyResolutionManagement {
    repositories { mavenCentral() }
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
