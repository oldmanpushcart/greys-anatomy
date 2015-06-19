package com.github.ompc.greys;

import com.github.ompc.greys.command.Commands;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.util.GaReflectUtils;
import jline.console.ConsoleReader;
import jline.console.completer.*;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.ompc.greys.util.GaStringUtils.DEFAULT_PROMPT;
import static java.io.File.separatorChar;
import static java.lang.System.getProperty;
import static jline.console.KeyMap.CTRL_D;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Greys控制台
 * Created by vlinux on 15/5/30.
 */
public class GreysConsole {

    private static final byte EOT = 0x04;
    private static final byte EOF = -1;

    // 5分钟
    private static final int _1MIN = 60 * 1000;

    // 工作目录
    private static final String WORKING_DIR = getProperty("user.home");

    // 历史命令存储文件
    private static final String HISTORY_FILENAME = ".greys_history";

    private final ConsoleReader console;
    private final History history;
    private final Writer out;

    private final Socket socket;
    private BufferedWriter socketWriter;
    private BufferedReader socketReader;

    private volatile boolean isRunning;


    public GreysConsole(InetSocketAddress address) throws IOException {

        this.console = initConsoleReader();
        this.history = initHistory();

        this.out = console.getOutput();
        this.history.moveToEnd();
        this.console.setHistoryEnabled(true);
        this.console.setHistory(history);
        this.console.addCompleter(initCompleter());

        this.socket = connect(address);

        this.isRunning = true;
        activeConsoleReader();
        loopForWriter();

    }


    private History initHistory() throws IOException {
        final File WORK_DIR = new File(WORKING_DIR);
        final File historyFile = new File(WORKING_DIR + separatorChar + HISTORY_FILENAME);
        if (WORK_DIR.canWrite()
                && WORK_DIR.canRead()
                && ((!historyFile.exists() && historyFile.createNewFile()) || historyFile.exists())) {
            return new FileHistory(historyFile);
        }
        return new MemoryHistory();
    }

    private ConsoleReader initConsoleReader() throws IOException {
        final ConsoleReader console = new ConsoleReader(System.in, System.out);
        console.getKeys().bind("" + CTRL_D, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    socketWriter.write(CTRL_D);
                    socketWriter.flush();
                } catch (Exception e1) {
                    // 这里是控制台，可能么？
                    GreysConsole.this.err("write fail : %s", e1.getMessage());
                    shutdown();
                }
            }

        });
        return console;
    }


    /*
     * 命令提示补全<br/>
     * 临时方案，根本方案是Console单独走一套协议
     */
    private Completer initCompleter() {

        final Collection<Completer> completerList = new ArrayList<Completer>();

        for (Map.Entry<String, Class<?>> entry : Commands.getInstance().listCommands().entrySet()) {
            ArgumentCompleter argCompleter = new ArgumentCompleter();
            completerList.add(argCompleter);
            argCompleter.getCompleters().add(new StringsCompleter(entry.getKey()));

            if (entry.getKey().equals("help")) {
                argCompleter.getCompleters().add(new StringsCompleter(Commands.getInstance().listCommands().keySet()));
            }

            for (Field field : GaReflectUtils.getFields(entry.getValue())) {
                if (field.isAnnotationPresent(NamedArg.class)) {
                    NamedArg arg = field.getAnnotation(NamedArg.class);
                    argCompleter.getCompleters().add(new StringsCompleter("-" + arg.name()));
                    if (field.getType().isEnum()) {
                        Enum<?>[] enums = (Enum[]) field.getType().getEnumConstants();
                        String[] enumArgs = new String[enums.length];
                        for (int i = 0; i < enums.length; i++) {
                            enumArgs[i] = enums[i].name();
                        }
                        argCompleter.getCompleters().add(new StringsCompleter(enumArgs));
                    } else {
                        argCompleter.getCompleters().add(new Completer() {

                            @Override
                            public int complete(String buffer, int cursor, List<CharSequence> candidates) {
                                if (null == buffer
                                        || buffer.isEmpty()
                                        || buffer.trim().isEmpty()) {
                                    candidates.add("");
                                    return 0;
                                } else {
                                    candidates.add(buffer + " ");
                                }
                                return 0;
                            }
                        });
                    }
                }
            }//for
            argCompleter.getCompleters().add(new NullCompleter());
        }

        return new AggregateCompleter(completerList);
    }


    /**
     * 激活网络
     */
    private Socket connect(InetSocketAddress address) throws IOException {
        final Socket socket = new Socket();
        socket.setSoTimeout(0);
        socket.connect(address, _1MIN);
        socket.setKeepAlive(true);
        socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return socket;
    }

    /**
     * 激活读线程
     */
    private void activeConsoleReader() {
        final Thread socketThread = new Thread("ga-console-reader-daemon") {

            @Override
            public void run() {
                try {

                    while (isRunning) {

                        final String line = console.readLine();
                        history.add(line);

                        // flush if need
                        if (history instanceof Flushable) {
                            ((Flushable) history).flush();
                        }

                        console.setPrompt(EMPTY);
                        if (isNotBlank(line)) {
                            socketWriter.write(line + "\n");
                        } else {
                            socketWriter.write("\n");
                        }
                        socketWriter.flush();

                    }
                } catch (IOException e) {
                    err("read fail : %s", e.getMessage());
                    shutdown();
                }

            }

        };
        socketThread.setDaemon(true);
        socketThread.start();
    }


    private volatile boolean hackingForReDrawPrompt = true;

    /*
     * Console在启动的时候会出现第一个提示符占位不准的BUG
     * 这个我无法很优雅的消除，所以这里做了一个小hacking
     */
    private void hackingForReDrawPrompt() {
        if (hackingForReDrawPrompt) {
            hackingForReDrawPrompt = false;
            System.out.println(EMPTY);
        }
    }

    private void loopForWriter() {

        try {
            while (isRunning) {
                final int c = socketReader.read();
                if (c == EOF) {
                    break;
                }
                if (c == EOT) {
                    hackingForReDrawPrompt();
                    console.setPrompt(DEFAULT_PROMPT);
                    console.redrawLine();
                } else {
                    out.write(c);
                }
                out.flush();
            }
        } catch (IOException e) {
            err("write fail : %s", e.getMessage());
            shutdown();
        }

    }

    private void err(String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    /**
     * 关闭Console
     */
    private void shutdown() {
        isRunning = false;
        closeQuietly(socketWriter);
        closeQuietly(socketReader);
        closeQuietly(socket);
        console.shutdown();
    }

    public static void main(String... args) throws IOException {
        new GreysConsole(new InetSocketAddress(args[0], Integer.valueOf(args[1])));
    }

}
