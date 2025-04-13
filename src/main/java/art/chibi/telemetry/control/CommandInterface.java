package art.chibi.telemetry.control;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface CommandInterface<S> {

    int SINGLE_SUCCESS = 1;

    /**
     * Executes a command.
     *
     * @param ctx The command context
     * @return an integer representing the command execution result
     * @throws CommandSyntaxException if a command syntax error occurs
     */
    int run(CommandContext<S> ctx) throws CommandSyntaxException;
}
