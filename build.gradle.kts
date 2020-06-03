plugins {
    id("uk.gov.justice.hmpps.gradle-spring-boot") version "0.4.1"
    kotlin("plugin.spring") version "1.3.72"
    kotlin("plugin.jpa") version "1.3.72"
}

group = "uk.gov.justice.digital.hmiprobation"

configurations {
    implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

ext ["spring-security.version"] = "5.3.2.RELEASE"

dependencies {

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("io.springfox:springfox-swagger2:2.9.2")
    implementation("io.springfox:springfox-swagger-ui:2.9.2")
    implementation("net.sf.ehcache:ehcache:2.10.6")
    implementation("org.apache.commons:commons-lang3:3.10")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.0")
    implementation( "com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")

    implementation("com.nimbusds:nimbus-jose-jwt:8.18")
    implementation("com.google.guava:guava:29.0-jre")


    testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.17.0")
    testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
    testImplementation("com.ninja-squad:springmockk:2.0.1")
    testImplementation("io.jsonwebtoken:jjwt:0.9.1")
    testImplementation("org.apache.poi:poi:4.1.2")
    testImplementation("org.apache.poi:poi-ooxml:4.1.2")

}