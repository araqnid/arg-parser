package org.araqnid.kotlin.argv

import kotlin.reflect.KProperty

class ArgParser(private val appName: String) {
    private val optionsByName = mutableMapOf<String, OptionProvider<*, *>>()
    private val optionsByShortName = mutableMapOf<String, OptionProvider<*, *>>()
    private val arguments = mutableListOf<ArgumentProvider<*, *>>()

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
                    TODO("long options")
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

    fun provideHelp(output: Appendable) {
        output.append("Syntax: ").append(appName)
        for (option in optionsByName.values) {
            if (option.argType.needsArgument) {
                output.append(" -${option.shortName} ${option.name}")
            }
            else {
                output.append(" -${option.shortName}")
            }
        }
        for (argument in arguments) {
            if (argument.multiple) {
                output.append(" ${argument.name}...")
            }
            else {
                output.append(" ${argument.name}")
            }
        }
    }

    private fun register(provider: OptionProvider<*, *>) {
        check(optionsByName[provider.name] == null) { "Option '--${provider.name}' already registered" }
        check(optionsByShortName[provider.shortName] == null) { "Option '-${provider.shortName}' already registered" }

        optionsByName[provider.name] = provider
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
        val description: String
    ) {
        abstract fun acceptArg(arg: String)

        abstract fun produce(): U

        operator fun getValue(owner: Any?, property: KProperty<*>): U = produce()
    }

    abstract inner class ArgumentProvider<out T, out U>(
        val argType: ArgType<T>,
        val name: String,
        val description: String,
        val multiple: Boolean
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
            val provider = object : OptionProvider<T, T?>(argType, shortName, property.name, description) {
                var value: T? = null
                var specified: Boolean = false

                override fun acceptArg(arg: String) {
                    if (specified) error("Option '$name' has already been specified")
                    value = argType.read(arg)
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
            val provider = object : OptionProvider<T, List<T>>(argType, shortName, property.name, description) {
                var values: List<T> = mutableListOf()

                override fun acceptArg(arg: String) {
                    values += argType.read(arg)
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
                register(object : OptionProvider<T, T>(argType, shortName, name, "description for $name") {
                    override fun acceptArg(arg: String) {
                        values += name to this.argType.read(arg)
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
            val provider = object : ArgumentProvider<T, T>(argType, property.name, description, multiple = false) {
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
            val provider = object : ArgumentProvider<T, T>(argType, property.name, description, multiple = false) {
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
            val provider = object : ArgumentProvider<T, T?>(argType, property.name, description, multiple = false) {
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
            val provider = object : ArgumentProvider<T, List<T>>(argType, property.name, description, multiple = true) {
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
            val provider = object : ArgumentProvider<T, List<T>>(argType, property.name, description, multiple = true) {
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
}

fun ArgParser.parse(args: Array<out String>) = parse(args.toList())
