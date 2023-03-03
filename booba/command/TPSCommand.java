package net.polar.command;

import net.minestom.server.command.builder.Command;
import net.polar.profiler.ServerProfiler;

public class TPSCommand extends Command {

    public TPSCommand() {
        super("tps", "serverinfo");
        addSyntax(((sender, context) -> {
            sender.sendMessage(ServerProfiler.getServerProfilerData());
        }));
    }


}
