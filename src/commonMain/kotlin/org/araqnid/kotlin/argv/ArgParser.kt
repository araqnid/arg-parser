package org.araqnid.kotlin.argv

import kotlin.reflect.KProperty

class ArgParser(private val appName: String) {
    private val optionsByName = mutableMapOf<String, OptionProvider<*, *>>()
    private val optionsByShortName = mutableMapOf<String, OptionProvider<*, *>>()
    private val arguments = mutableListOf<ArgumentProvider<*, *>>()
    var helpNeeded = false
        private set

    init {
        val help = object : OptionProvider<Boolean, Boolean>(ArgType.BOOLEAN, "h", "help", "Provide syntax summary") {
            override fun acceptArg(arg: String) {
                helpNeeded = argType.read(arg)
            }

            override fun appendSyntaxSummary(output: Appendable) {
            }

            override fun produce(): Boolean {
                return helpNeeded
            }
        }
        register(help)
    }

    fun parse(args: Iterable<String>) {
        val inputIterator = args.iterator()
        var inArgumentList = false
        var currentArgument = 0
        while (inputIterator.hasNext()) {
            val inputString = inputIterator.next()
            if (inputString == "--") {
                inArgumentList = true
            }
            else if (!inArgumentList) {
                if (inputString.startsWith("--")) {
                    val valueSeparator = inputString.indexOf('=')
                    if (valueSeparator < 0) {
                        val optionName = inputString.substring(2)
                        val option = optionsByName[optionName] ?: error("Unknown option '--$optionName'")
                        if (option.argType.needsArgument) {
                            option.acceptArg(inputIterator.next())
                        }
                        else {
                            option.acceptArg("")
                        }
                    }
                    else {
                        val optionName = inputString.substring(2 until valueSeparator)
                        val option = optionsByName[optionName] ?: error("Unknown option '--$optionName'")
                        option.acceptArg(inputString.substring(valueSeparator + 1))
                    }
                    continue
                }
                else if (inputString.startsWith("-")) {
                    val firstOption = findOption(inputString[1].toString())
                    if (firstOption.argType.needsArgument) {
                        if (inputString.length == 2) {
                            val arg = inputIterator.next()
                            firstOption.acceptArg(arg)
                        }
                        else {
                            val arg = inputString.substring(2)
                            firstOption.acceptArg(arg)
                        }
                    }
                    else {
                        for (i in 1 until inputString.length) {
                            val provider = findOption(inputString[i].toString())
                            if (provider.argType.needsArgument) {
                                error("Option '${provider.name}' needs an argument")
                            }
                            provider.acceptArg("")
                        }
                    }
                    continue
                }
            }
            else {
                inArgumentList = true
            }
            check(currentArgument < arguments.size) { "Unexpected trailing argument: $inputString" }
            val provider = arguments[currentArgument]
            provider.acceptArg(inputString)
            if (!provider.multiple) currentArgument++
        }
    }

    fun buildSyntax(): String {
        return buildString {
            val seenGroups = mutableSetOf<String>()
            for (option in optionsByName.values) {
                if (!seenGroups.add(option.group)) continue
                append(' ')
                option.appendSyntaxSummary(this)
            }
            for (argument in arguments) {
                when {
                    argument.multiple && argument.optional -> {
                        append(" [${argument.name}...]")
                    }
                    argument.multiple -> {
                        append(" ${argument.name}...")
                    }
                    argument.optional -> {
                        append(" [${argument.name}]")
                    }
                    else -> {
                        append(" ${argument.name}")
                    }
                }
            }
        }.trimStart()
    }

    private fun register(provider: OptionProvider<*, *>) {
        check(optionsByName[provider.longName] == null) { "Option '--${provider.longName}' already registered" }
        check(optionsByShortName[provider.shortName] == null) { "Option '-${provider.shortName}' already registered" }

        optionsByName[provider.longName] = provider
        optionsByShortName[provider.shortName] = provider
    }

    private fun register(provider: ArgumentProvider<*, *>) {
        arguments += provider
    }

    private fun findOption(shortName: String): OptionProvider<*, *> {
        return optionsByShortName[shortName] ?: error("Unknown option: -$shortName")
    }

    fun <T> option(type: ArgType<T>, shortName: String, description: String): OptionalSingleOptionDefinition<T> {
        return OptionalSingleOptionDefinition(type, shortName, description)
    }

    fun <T> optionGroup(type: ArgType<T>, vararg options: Pair<String, String>): OptionGroupDefinition<T> {
        return OptionGroupDefinition(type, options.toList())
    }

    fun <T> argument(type: ArgType<T>, description: String): MandatoryArgumentDefinition<T> {
        return MandatoryArgumentDefinition(type, description)
    }

    abstract inner class OptionProvider<out T, out U>(
        val argType: ArgType<T>,
        val shortName: String,
        val name: String,
        val description: String,
        val group: String = name
    ) {
        val longName = camelCaseToDashes(name)

        abstract fun acceptArg(arg: String)

        abstract fun produce(): U

        abstract fun appendSyntaxSummary(output: Appendable)

        operator fun getValue(owner: Any?, property: KProperty<*>): U = produce()
    }

    abstract inner class ArgumentProvider<out T, out U>(
        val argType: ArgType<T>,
        val name: String,
        val description: String,
        val multiple: Boolean,
        val optional: Boolean
    ) {
        abstract fun acceptArg(arg: String)

        abstract fun produce(): U

        operator fun getValue(owner: Any?, property: KProperty<*>): U = produce()
    }

    inner class DefaultedSingleOptionDefinition<T>(
        val argType: ArgType<T>,
        val shortName: String,
        val description: String,
        val defaultProvider: () -> T
    ) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): OptionProvider<T, T> {
            val provider = object : OptionProvider<T, T>(argType, shortName, property.name, description) {
                var value: T? = null
                var specified: Boolean = false

                override fun acceptArg(arg: String) {
                    if (specified) error("Option '$name' has already been specified")
                    value = argType.read(arg)
                }

                override fun appendSyntaxSummary(output: Appendable) {
                    output.append("")
                }

                override fun produce(): T = value ?: defaultProvider()
            }
            register(provider)
            return provider
        }
    }

    inner class OptionalSingleOptionDefinition<T>(
        val argType: ArgType<T>,
        val shortName: String,
        val description: String
    ) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): OptionProvider<T, T?> {
            val provider = object : OptionProvider<T, T?>(argType, shortName, property.name, description
            ) {
                var value: T? = null
                var specified: Boolean = false

                override fun acceptArg(arg: String) {
                    if (specified) error("Option '$name' has already been specified")
                    value = argType.read(arg)
                }

                override fun appendSyntaxSummary(output: Appendable) {
                    output.apply {
                        append("[ -")
                        append(shortName)
                        if (argType.needsArgument) append(" <$name>")
                        append(" | --")
                        append(longName)
                        if (argType.needsArgument) append("=<value>")
                        append(" ]")
                    }
                }

                override fun produce(): T? = value
            }
            register(provider)
            return provider
        }

        fun default(defaultValue: T): DefaultedSingleOptionDefinition<T> {
            return DefaultedSingleOptionDefinition(argType, shortName, description) { defaultValue }
        }

        fun default(defaultProvider: () -> T): DefaultedSingleOptionDefinition<T> {
            return DefaultedSingleOptionDefinition(argType, shortName, description, defaultProvider)
        }

        fun multiple(): MultipleOptionDefinition<T> {
            return MultipleOptionDefinition(argType, shortName, description)
        }
    }

    inner class MultipleOptionDefinition<T>(
        val argType: ArgType<T>,
        val shortName: String,
        val description: String
    ) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): OptionProvider<T, List<T>> {
            val provider = object : OptionProvider<T, List<T>>(argType, shortName, property.name, description
            ) {
                val values: MutableList<T> = mutableListOf()

                override fun acceptArg(arg: String) {
                    values += argType.read(arg)
                }

                override fun appendSyntaxSummary(output: Appendable) {
                    output.apply {
                        append("[ -")
                        append(shortName)
                        if (argType.needsArgument) append(" <$name>")
                        append(" | --")
                        append(longName)
                        if (argType.needsArgument) append("=<value>")
                        append(" ]...")
                    }
                }

                override fun produce(): List<T> = values
            }
            register(provider)
            return provider
        }
    }

    class GroupCollector<T>(private val values: List<Pair<String, T>>) {
        operator fun getValue(owner: Any?, property: KProperty<*>): List<Pair<String, T>> = values
    }

    inner class OptionGroupDefinition<T>(val argType: ArgType<T>, val options: List<Pair<String, String>>) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): GroupCollector<T> {
            val values = mutableListOf<Pair<String, T>>()
            for ((name, shortName) in options) {
                register(object : OptionProvider<T, T>(argType, shortName, name, "description for $name", group = options[0].first) {
                    override fun acceptArg(arg: String) {
                        values += name to this.argType.read(arg)
                    }

                    override fun appendSyntaxSummary(output: Appendable) {
                        output.apply {
                            options.joinTo(output, " | ", prefix = "[ ", postfix = " ]...") { "-${it.second} | --${it.first}" }
                        }
                    }

                    override fun produce(): T {
                        throw UnsupportedOperationException()
                    }
                })
            }
            return GroupCollector(values)
        }
    }

    inner class MandatoryArgumentDefinition<T>(val argType: ArgType<T>, val description: String) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): ArgumentProvider<T, T> {
            val provider = object : ArgumentProvider<T, T>(argType, property.name, description, multiple = false, optional = false) {
                var value: T? = null
                var specified: Boolean = false

                override fun acceptArg(arg: String) {
                    if (specified) error("Argument '$name' has already been specified")
                    value = argType.read(arg)
                }

                @Suppress("UNCHECKED_CAST")
                override fun produce(): T = if (specified) value as T else error("Argument '$name' has not been specified")
            }
            register(provider)
            return provider
        }

        fun optional(): OptionalArgumentDefinition<T> {
            return OptionalArgumentDefinition(argType, description)
        }

        fun default(defaultValue: T): DefaultedArgumentDefinition<T> {
            return DefaultedArgumentDefinition(argType, description) { defaultValue }
        }

        fun default(defaultProvider: () -> T): DefaultedArgumentDefinition<T> {
            return DefaultedArgumentDefinition(argType, description, defaultProvider)
        }

        fun vararg(): MandatoryVarargArgumentDefinition<T> {
            return MandatoryVarargArgumentDefinition(argType, description)
        }
    }

    inner class DefaultedArgumentDefinition<T>(val argType: ArgType<T>, val description: String, val defaultProvider: () -> T) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): ArgumentProvider<T, T> {
            val provider = object : ArgumentProvider<T, T>(argType, property.name, description, multiple = false, optional = true) {
                var value: T? = null
                var specified: Boolean = false

                override fun acceptArg(arg: String) {
                    if (specified) error("Argument '$name' has already been specified")
                    value = argType.read(arg)
                    specified = true
                }

                @Suppress("UNCHECKED_CAST")
                override fun produce(): T = if (specified) value as T else defaultProvider()
            }
            register(provider)
            return provider
        }

        fun optional(): OptionalArgumentDefinition<T> {
            return OptionalArgumentDefinition(argType, description)
        }
    }

    inner class OptionalArgumentDefinition<T>(val argType: ArgType<T>, val description: String) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): ArgumentProvider<T, T?> {
            val provider = object : ArgumentProvider<T, T?>(argType, property.name, description, multiple = false, optional = true) {
                var value: T? = null
                var specified: Boolean = false

                override fun acceptArg(arg: String) {
                    if (specified) error("Argument '$name' has already been specified")
                    value = argType.read(arg)
                    specified = true
                }

                override fun produce(): T? = value
            }
            register(provider)
            return provider
        }

        fun vararg(): OptionalVarargArgumentDefinition<T> {
            return OptionalVarargArgumentDefinition(argType, description)
        }
    }

    inner class MandatoryVarargArgumentDefinition<T>(val argType: ArgType<T>, val description: String) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): ArgumentProvider<T, List<T>> {
            val provider = object : ArgumentProvider<T, List<T>>(argType, property.name, description, multiple = true, optional = false) {
                val values: MutableList<T> = mutableListOf()

                override fun acceptArg(arg: String) {
                    values += argType.read(arg)
                }

                override fun produce(): List<T> = if (values.isNotEmpty()) values else error("No values specified for '$name'")
            }
            register(provider)
            return provider
        }
    }

    inner class OptionalVarargArgumentDefinition<T>(val argType: ArgType<T>, val description: String) {
        operator fun provideDelegate(owner: Any?, property: KProperty<*>): ArgumentProvider<T, List<T>> {
            val provider = object : ArgumentProvider<T, List<T>>(argType, property.name, description, multiple = true, optional = true) {
                val values: MutableList<T> = mutableListOf()

                override fun acceptArg(arg: String) {
                    values += argType.read(arg)
                }

                override fun produce(): List<T> = values
            }
            register(provider)
            return provider
        }
    }

    companion object {
        internal fun camelCaseToDashes(input: CharSequence): String {
            return buildString {
                var lastWasLower = false
                for (c in input) {
                    if (c.isSimpleUpperCase()) {
                        if (lastWasLower) {
                            lastWasLower = false
                            append('-')
                        }
                        append(c.toSimpleLowerCase())
                    }
                    else {
                        append(c)
                        lastWasLower = true
                    }
                }
            }
        }

        private fun Char.isSimpleUpperCase() = this in 'A'..'Z'
        private fun Char.toSimpleLowerCase(): Char {
            return if (this in 'A'..'Z') {
                val offset = this - 'A'
                "abcdefghijklmnopqrstuvwxyz"[offset]
            } else {
                this
            }
        }
    }
}
