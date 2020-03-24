package org.araqnid.kotlin.argv

interface ArgType<out T> {
    fun read(str: String): T
    val needsArgument
        get() = true

    companion object {
        val BOOLEAN = object : ArgType<Boolean> {
            override fun read(str: String): Boolean = str == "" || str.toBoolean()
            override val needsArgument: Boolean
                get() = false
        }

        val STRING = object : ArgType<String> {
            override fun read(str: String): String = str
        }

        val INTEGER = object : ArgType<Int> {
            override fun read(str: String): Int = str.toInt()
        }
    }
}

@Suppress("FunctionName")
fun <T> ArgType(reader: (String) -> T): ArgType<T> {
    return object : ArgType<T> {
        override fun read(str: String): T = reader(str)
        override fun toString(): String = reader.toString()
    }
}
