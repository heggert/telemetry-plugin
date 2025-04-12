package art.chibi.telemetry

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.jspecify.annotations.NullMarked
import java.util.concurrent.CompletableFuture

@NullMarked
object TelemetryCommand {

    fun constructGiveItemCommand(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("throwexception")
            .then(
                Commands.argument("type", IntegerArgumentType.integer(1, 2))
                    .suggests(::getTypeSuggestions)
                    .executes(::testRandomException)
            )
            .build()
    }

    private fun getTypeSuggestions(
        ctx: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        builder.suggest(1)
        builder.suggest(2)
        return builder.buildFuture()
    }

    /**
     * Throws a random exception to test exception logging.
     */
    private fun testRandomException(ctx: CommandContext<CommandSourceStack>): Int {
        val type = IntegerArgumentType.getInteger(ctx, "type")
        return when (type) {
            1 -> throw IllegalStateException("Test exception: IllegalStateException triggered!")
            2 -> throw NullPointerException("Test exception: NullPointerException triggered!")
            else -> Command.SINGLE_SUCCESS
        }
    }
}
