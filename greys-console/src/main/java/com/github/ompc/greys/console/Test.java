package com.github.ompc.greys.console;

import picocli.CommandLine;

import java.util.List;

public class Test {

    @CommandLine.Command(
            subcommands = Mixed.class
    )
    static class Ga {

    }

    @CommandLine.Command(name = "mix")
    static class Mixed {
        @CommandLine.Parameters(index = "0")
        String positional0;

        @CommandLine.Parameters(index = "1")
        String positional1;

        @CommandLine.Parameters(index = "2")
        String positional2;

        // @CommandLine.Parameters(index = "*")
        // String[] positionalOther;

        @CommandLine.Option(names = "-o")
        List<String> options;
    }

    public static void main(String... args) {
        String[] _args = {"param0", "-o", "AAA", "param1", "param2", "-o", "BBB", "param3"};
        Mixed mixed = new Mixed();
        new CommandLine(mixed)
                .setStopAtUnmatched(true)
                .parse(_args);
        System.out.println(mixed.positional2);
    }

}
