package net.oneandone.pommes.cli;


import net.oneandone.inline.Console;
import net.oneandone.setenv.Setenv;

public class Setup {
    private final Console console;

    public Setup(Console console) {
        this.console = console;
    }

    public void run() {
        console.info.println(Setenv.get().setenvBash());
    }
}
