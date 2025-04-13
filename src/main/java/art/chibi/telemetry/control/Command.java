package art.chibi.telemetry.control;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public final class Command {

    // Utility class; private constructor to prevent instantiation
    private Command() {
    }

    public static LiteralCommandNode<CommandSourceStack> constructGiveItemCommand() {
        return Commands.literal("throwexception")
                .then(
                        Commands.argument("type", IntegerArgumentType.integer(1, 2))
                                .suggests(Command::getTypeSuggestions)
                                .executes(Command::testRandomException)
                )
                .build();
    }

    private static CompletableFuture<Suggestions> getTypeSuggestions(
            CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder
    ) {
        builder.suggest(1);
        builder.suggest(2);
        return builder.buildFuture();
    }

    /**
     * Throws a random exception to test exception logging.
     */
    private static int testRandomException(CommandContext<CommandSourceStack> ctx) {
        int type = IntegerArgumentType.getInteger(ctx, "type");
        switch (type) {
            case 1:
                throw new IllegalStateException("Test exception: IllegalStateException triggered!");
            case 2:
                throw new NullPointerException("Test exception: NullPointerException triggered!");
            default:
                // Replace CommandInterface.SINGLE_SUCCESS with the actual constant/return value as needed
                return CommandInterface.SINGLE_SUCCESS;
        }
    }
}
