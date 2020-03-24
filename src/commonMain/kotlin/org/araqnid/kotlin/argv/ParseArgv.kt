package org.araqnid.kotlin.argv

fun ArgParser.parse(args: Array<out String>) {
    parse(args.toList())
}

fun ArgParser.parseArgv(args: Array<out String>): Boolean {
    parse(args.toList())
    return if (helpNeeded) {
        print("Options: " + buildSyntax())
        false
    }
    else {
        true
    }
}
