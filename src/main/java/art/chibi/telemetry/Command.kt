package art.chibi.telemetry

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException

fun interface Command<S> {
    companion object {
        const val SINGLE_SUCCESS = 1
    }

    @Throws(CommandSyntaxException::class)
    fun run(ctx: CommandContext<S>): Int
}