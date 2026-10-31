plugins { java }
java { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
dependencies {
    testImplementation(libs.test.junit)
    testImplementation(libs.test.pdfbox)
}
tasks.test {
    systemProperty("pdf.fixture.dir", providers.gradleProperty("pdfFixtureDir").getOrElse("build/device-pdfs"))
}
