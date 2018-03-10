package com.github.ompc.greys.console;

import lombok.Data;
import picocli.CommandLine;

/**
 * Greys控制台配置
 */
@Data
public class GaConsoleConfig {

    @CommandLine.Option(names = "--ip",
            paramLabel = "IP",
            description = "connect to target server's IP",
            required = true,
            usageHelp = true
    )
    private String ip;

    @CommandLine.Option(names = "--port",
            paramLabel = "PORT",
            description = "connect to target server's PORT",
            required = true,
            usageHelp = true
    )
    private int port;

    @CommandLine.Option(names = "--namespace",
            paramLabel = "NAMESPACE",
            description = "target jvm-sandbox's NAMESPACE, default is \"greys\"",
            usageHelp = true
    )
    private String namespace = "greys";

    @CommandLine.Option(names = "--connect-timeout",
            paramLabel = "CONNECT-TIMEOUT(sec)",
            description = "connect to server timeout. default is \"10\"",
            usageHelp = true
    )
    private int connectTimeoutSec = 10;

    @CommandLine.Option(names = "--timeout",
            paramLabel = "TIMEOUT(sec)",
            description = "communication with server timeout. default is \"60\"",
            usageHelp = true
    )
    private int timeoutSec = 60;

}
