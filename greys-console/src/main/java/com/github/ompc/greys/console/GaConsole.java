package com.github.ompc.greys.console;

import com.github.ompc.greys.console.command.GaCommands;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.io.File.separatorChar;
import static java.lang.System.getProperty;
import static jline.console.KeyMap.CTRL_D;
import static jline.internal.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class GaConsole {

    private static final Logger logger = LoggerFactory.getLogger(GaConsole.class);

    private static final String PROMPT = "ga?>";

    private final ConsoleReader consoleReader;
    private final ConsoleWriter consoleWriter;
    private State state = State.WAITING_INPUT;

    protected GaConsole(InterruptCallback interruptCb) throws IOException {
        this.consoleReader = new ConsoleReader(System.in, System.out);
        this.consoleWriter = new ConsoleWriter(consoleReader.getOutput());

        bindPrompt();
        bindInterrupt(interruptCb);
        bindAutoCompleter();
        bindHistory();
        turnOffExpand();

    }

    private void bindPrompt() {
        consoleReader.setPrompt(PROMPT);
    }

    private void bindInterrupt(final InterruptCallback interruptCb) {
        consoleReader.getKeys().bind("" + CTRL_D, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interruptCb.interrupt();
            }
        });
    }

    private void bindAutoCompleter() {
        consoleReader.addCompleter(new Completer() {

            final SortedSet<String> commands = new TreeSet<String>(
                    GaCommands.instance.names()
            );

            @Override
            public int complete(String buffer, int cursor, List<CharSequence> candidates) {

                // buffer could be null
                checkNotNull(candidates);

                // 当前输入内容为空，直接按<TAB>，则推荐所有当前可执行的命令
                if (buffer == null) {
                    candidates.addAll(commands);
                }

                // 当前输入内容已经有值，则根据已输入的内容对先有命令做前置匹配
                else {

                    // 获取当前输入内容作为前缀
                    final String prefix = buffer.length() > cursor
                            ? buffer.substring(0, cursor)
                            : buffer;

                    for (String match : commands.tailSet(prefix)) {
                        if (!match.startsWith(prefix)) {
                            break;
                        }
                        candidates.add(match);
                    }
                }

                // 如果只匹配出一个建议值，则将当前建议值作为当前输入内容的补充
                if (candidates.size() == 1) {
                    candidates.set(0, candidates.get(0) + " ");
                }

                // 没有匹配上任何的推荐命令补充
                return candidates.isEmpty()
                        ? -1
                        : 0;
            }

        });
    }

    private void bindHistory() {
        final String WORKING_DIR = getProperty("user.home");
        final String HISTORY_FILENAME = ".greys_history";
        final File workDir = new File(WORKING_DIR);
        final File historyFile = new File(WORKING_DIR + separatorChar + HISTORY_FILENAME);
        try {
            final History history =
                    workDir.canWrite()
                            && workDir.canRead()
                            && ((!historyFile.exists() && historyFile.createNewFile()) || historyFile.exists())
                            ? new FileHistory(historyFile)
                            : new MemoryHistory();
            history.moveToEnd();
            consoleReader.setHistory(history);
            consoleReader.setHistoryEnabled(true);
        } catch (IOException ioCause) {
            logger.warn("console init history: {} failed.", historyFile, ioCause);
        }
    }

    private void turnOffExpand() {
        consoleReader.setExpandEvents(false);
    }


    /**
     * 获取控制台读
     *
     * @return 控制台读
     */
    public ConsoleReader getConsoleReader() {
        return consoleReader;
    }

    /**
     * 获取控制台写
     *
     * @return 控制台写
     */
    public ConsoleWriter getConsoleWriter() {
        return consoleWriter;
    }

    /**
     * 如果有必要，刷新History文件
     *
     * @throws IOException 刷新History文件失败
     */
    public void flushHistoryIfNecessary() throws IOException {
        final History history = consoleReader.getHistory();
        if (history instanceof Flushable) {
            ((Flushable) history).flush();
        }
    }

    /**
     * 获取控制台状态
     *
     * @return 控制台状态
     */
    public State getState() {
        return state;
    }

    /**
     * 变更控制台状态
     *
     * @param state 控制台状态
     */
    public void changeState(State state) {
        this.state = state;
        switch (state) {
            case WAITING_INPUT:
                consoleReader.setPrompt(PROMPT);
                resetPromptLineQuietly();
                break;
            case WAITING_COMMAND:
                consoleReader.setPrompt(EMPTY);
                break;
        }
    }

    private void resetPromptLineQuietly() {
        try {
            consoleReader.resetPromptLine(consoleReader.getPrompt(), EMPTY, 0);
        } catch (IOException e) {
            // ignore...
        }
    }


    /**
     * 控制台写
     */
    public class ConsoleWriter extends PrintWriter {

        ConsoleWriter(Writer writer) {
            super(writer, true);
        }

    }

    /**
     * 中断回调
     */
    public interface InterruptCallback {

        /**
         * 中断等待命令运行被中断
         */
        void interrupt();

    }

    /**
     * 控制台状态
     */
    public enum State {

        /**
         * 等待用户输入
         */
        WAITING_INPUT,

        /**
         * 等待命令完结
         */
        WAITING_COMMAND

    }

}
