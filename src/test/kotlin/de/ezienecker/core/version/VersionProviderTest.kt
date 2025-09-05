package de.ezienecker.core.version

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll

class VersionProviderTest : FunSpec({

    afterEach {
        unmockkAll()
    }

    test("should return the version from properties file when application.properties exists and contains version") {
        VersionProvider.version shouldBe "1.2.3-TEST"
    }

    test("should return 'unknown' as fallback when application.properties is missing") {
        mockkObject(VersionProvider)

        every { VersionProvider.getPropertiesFile() } returns ""

        VersionProvider.version shouldBe "unknown"
    }

    test("should return 'unknown' as fallback when properties file exists but version property is missing") {
        mockkObject(VersionProvider)

        every { VersionProvider.getPropertiesFile() } returns "application-without-version.properties"

        VersionProvider.version shouldBe "unknown"
    }


    test("should return empty string from properties when properties file has empty version value") {
        mockkObject(VersionProvider)

        every { VersionProvider.getPropertiesFile() } returns "application-empty-version.properties"

        VersionProvider.version shouldBe "unknown"
    }

    test("should not be empty and return consistent results when version property is accessed multiple times") {
        val firstAccess = VersionProvider.version
        val secondAccess = VersionProvider.version

        firstAccess.shouldNotBeEmpty()
        secondAccess shouldBe firstAccess
    }
})
