package org.araqnid.kotlin.argv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ArgParserTest {
    @Test
    fun accepts_empty_args() {
        ArgParser("test").parse(listOf())
    }

    @Test
    fun accepts_clustered_switches() {
        val parser = ArgParser("test")
        val verbose by parser.option(ArgType.BOOLEAN, "v", "Verbose mode")
        val dryRun by parser.option(ArgType.BOOLEAN, "n", "Dry-run mode")
        parser.parse(listOf("-nv"))
        assertEquals(true, verbose)
        assertEquals(true, dryRun)
    }

    @Test
    fun accepts_option_with_attached_value() {
        val parser = ArgParser("test")
        val environment by parser.option(ArgType.STRING, "e", "Environment")
        val application by parser.option(ArgType.STRING, "a", "Application")
        parser.parse(listOf("-elatest", "-aTestApp"))
        assertEquals("latest", environment)
        assertEquals("TestApp", application)
    }

    @Test
    fun accepts_option_with_subsequent_value() {
        val parser = ArgParser("test")
        val environment by parser.option(ArgType.STRING, "e", "Environment")
        val application by parser.option(ArgType.STRING, "a", "Application")
        parser.parse(listOf("-e", "latest", "-a", "TestApp"))
        assertEquals("latest", environment)
        assertEquals("TestApp", application)
    }

    @Test
    fun accepts_long_option_for_valueless_option() {
        val parser = ArgParser("test")
        val verbose by parser.option(ArgType.BOOLEAN, "v", "Verbose mode")
        val dryRun by parser.option(ArgType.BOOLEAN, "n", "Dry-run mode")
        parser.parse(listOf("--verbose", "--dry-run"))
        assertEquals(true, verbose)
        assertEquals(true, dryRun)
    }

    @Test
    fun accepts_long_option_with_attached_value() {
        val parser = ArgParser("test")
        val environment by parser.option(ArgType.STRING, "e", "Environment")
        val application by parser.option(ArgType.STRING, "a", "Application")
        parser.parse(listOf("--environment=latest", "--application=TestApp"))
        assertEquals("latest", environment)
        assertEquals("TestApp", application)
    }

    @Test
    fun accepts_long_option_with_subsequent_value() {
        val parser = ArgParser("test")
        val environment by parser.option(ArgType.STRING, "e", "Environment")
        val application by parser.option(ArgType.STRING, "a", "Application")
        parser.parse(listOf("--environment", "latest", "--application", "TestApp"))
        assertEquals("latest", environment)
        assertEquals("TestApp", application)
    }

    @Test
    fun missing_options_provided_as_null() {
        val parser = ArgParser("test")
        val environment by parser.option(ArgType.STRING, "e", "Environment")
        val application by parser.option(ArgType.STRING, "a", "Application")
        parser.parse(listOf())
        assertNull(environment)
        assertNull(application)
    }

    @Test
    fun missing_options_can_be_given_a_default_value() {
        val parser = ArgParser("test")
        val environment by parser.option(ArgType.STRING, "e", "Environment").default("latest")
        val application by parser.option(ArgType.STRING, "a", "Application").default("TestApp")
        parser.parse(listOf())
        assertEquals("latest", environment)
        assertEquals("TestApp", application)
    }

    @Test
    fun missing_options_can_be_given_a_default_value_as_a_provider_lambda() {
        val parser = ArgParser("test")
        val environment by parser.option(ArgType.STRING, "e", "Environment").default { "latest" }
        val application by parser.option(ArgType.STRING, "a", "Application").default { "TestApp" }
        parser.parse(listOf())
        assertEquals("latest", environment)
        assertEquals("TestApp", application)
    }

    @Test
    fun options_can_be_given_multiple_times() {
        val parser = ArgParser("test")
        val filter by parser.option(ArgType.STRING, "f", "Filter").multiple()
        parser.parse(listOf("-fred", "-fblue"))
        assertEquals(listOf("red", "blue"), filter)
    }

    @Test
    fun parses_integer_value() {
        val parser = ArgParser("test")
        val forkLimit by parser.option(ArgType.INTEGER, "j", "Fork limit")
        parser.parse(listOf("-j4"))
        assertEquals(4, forkLimit)
    }

    @Test
    fun custom_parser_for_numeric_value() {
        val parser = ArgParser("test")
        val forkLimit by parser.option(ArgType { it.toInt() }, "j", "Fork limit")
        parser.parse(listOf("-j4"))
        assertEquals(4, forkLimit)
    }

    @Test
    fun group_together_related_switches() {
        val parser = ArgParser("test")
        val actions by parser.optionGroup(ArgType.BOOLEAN,
            "status" to "s",
            "resolve" to "r"
        )
        parser.parse(listOf("-srs"))
        assertEquals(listOf("status" to true, "resolve" to true, "status" to true), actions)
    }

    @Test
    fun arguments_are_mandatory() {
        val parser = ArgParser("test")
        val inputDir by parser.argument(ArgType.STRING, "input directory")
        assertFailsWith<IllegalStateException> {
            parser.parse(listOf())
            inputDir
        }
    }

    @Test
    fun arguments_can_be_optional() {
        val parser = ArgParser("test")
        val inputDir by parser.argument(ArgType.STRING, "input directory").optional()
        parser.parse(listOf())
        assertNull(inputDir)
    }

    @Test
    fun arguments_can_have_a_default_value() {
        val parser = ArgParser("test")
        val inputDir by parser.argument(ArgType.STRING, "input directory").default("inputs")
        parser.parse(listOf())
        assertEquals("inputs", inputDir)
    }

    @Test
    fun arguments_can_have_a_default_value_provider() {
        val parser = ArgParser("test")
        val inputDir by parser.argument(ArgType.STRING, "input directory").default { "inputs" }
        parser.parse(listOf())
        assertEquals("inputs", inputDir)
    }

    @Test
    fun arguments_can_take_multiple_values() {
        val parser = ArgParser("test")
        val actions by parser.argument(ArgType.STRING, "action").vararg()
        parser.parse(listOf("build", "publish"))
        assertEquals(listOf("build", "publish"), actions)
    }

    @Test
    fun vararg_arguments_are_still_required() {
        val parser = ArgParser("test")
        val actions by parser.argument(ArgType.STRING, "action").vararg()
        assertFailsWith<IllegalStateException> {
            parser.parse(listOf())
            actions
        }
    }

    @Test
    fun vararg_arguments_may_be_optional() {
        val parser = ArgParser("test")
        val actions by parser.argument(ArgType.STRING, "action").optional().vararg()
        parser.parse(listOf())
        assertEquals(listOf(), actions)
    }
}
