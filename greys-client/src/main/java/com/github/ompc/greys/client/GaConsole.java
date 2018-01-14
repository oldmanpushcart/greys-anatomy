package com.github.ompc.greys.client;

import com.github.ompc.greys.client.command.CommandInitializationException;
import com.github.ompc.greys.client.command.CommandParser;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.io.File.separatorChar;
import static java.lang.System.getProperty;
import static jline.console.KeyMap.CTRL_D;
import static jline.internal.Preconditions.checkNotNull;

/**
 * Greys命令控制台
 */
public class GaConsole {

    private static final Logger logger = LoggerFactory.getLogger(GaConsole.class);

    // 工作目录
    private static final String WORKING_DIR = getProperty("user.home");

    // 历史命令存储文件
    private static final String HISTORY_FILENAME = ".greys_history";

    private final ConsoleReader console;
    private final History history;
    private final Writer out;

    private final CommandParser parser;
    private volatile boolean isRunning;

    private GaConsole(final String ip,
                      final int port,
                      final String namespace) throws IOException, CommandInitializationException {
        this.console = initConsoleReader();
        this.history = initHistory();
        this.out = console.getOutput();
        this.parser = new CommandParser();

        // 初始化自动补全
        initCompleter();

        // 注入History
        this.console.setHistoryEnabled(true);
        this.console.setHistory(this.history);

        // xxx
        this.console.setExpandEvents(false);

        // 激活控制台
        this.isRunning = true;

    }

    private ConsoleReader initConsoleReader() throws IOException {
        final ConsoleReader console = new ConsoleReader(System.in, System.out);

        console.getKeys().bind("" + CTRL_D, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

            }

        });

        return console;
    }

    private History initHistory() throws IOException {
        final File workDir = new File(WORKING_DIR);
        final File historyFile = new File(WORKING_DIR + separatorChar + HISTORY_FILENAME);
        final History history;
        if (workDir.canWrite()
                && workDir.canRead()
                && ((!historyFile.exists() && historyFile.createNewFile()) || historyFile.exists())) {
            history = new FileHistory(historyFile);
        } else {
            history = new MemoryHistory();
        }
        history.moveToEnd();
        return history;
    }

    // jLine的自动补全
    private void initCompleter() {
        final SortedSet<String> commands = new TreeSet<String>();
        commands.addAll(parser.getCommandMap().keySet());

        console.addCompleter(new Completer() {
            @Override
            public int complete(String buffer, int cursor, List<CharSequence> candidates) {
                // buffer could be null
                checkNotNull(candidates);

                if (buffer == null) {
                    candidates.addAll(commands);
                } else {
                    String prefix = buffer;
                    if (buffer.length() > cursor) {
                        prefix = buffer.substring(0, cursor);
                    }
                    for (String match : commands.tailSet(prefix)) {
                        if (!match.startsWith(prefix)) {
                            break;
                        }
                        candidates.add(match);
                    }
                }

                if (candidates.size() == 1) {
                    candidates.set(0, candidates.get(0) + " ");
                }

                return candidates.isEmpty() ? -1 : 0;
            }

        });
    }


    /**
     * GreysClient主入口
     *
     * @param args $1 : 目标服务器IP地址
     *             $2 : 目标服务器端口号
     *             $3 : 目标jvm-sandbox命名空间
     */
    public static void main(String... args) {

        final String ip;
        final int port;
        final String namespace;

        if (args.length >= 2
                && StringUtils.isNotBlank(args[1])) {
            ip = args[1];
        } else {
            throw new IllegalArgumentException("{IP} argument was missing");
        }

        if (args.length >= 3
                && NumberUtils.isNumber(args[2])) {
            port = NumberUtils.toInt(args[2]);
        } else {
            throw new IllegalArgumentException("{PORT} argument was missing");
        }

        if (args.length >= 4
                && StringUtils.isNotBlank(args[3])) {
            namespace = args[3];
        } else {
            namespace = "default";
        }

        logger.info("greys-console was prepare to launch. ip={};port={};namespace={};",
                ip, port, namespace);

    }

}
