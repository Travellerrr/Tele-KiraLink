package cn.travellerr.onebottelegram.command;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.config.ConfigGenerator;
import cn.travellerr.onebottelegram.hibernate.entity.Message;
import cn.travellerr.onebottelegram.webui.api.LogWebSocketHandler;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandHandler {

    public static final CommandHandler INSTANCE = new CommandHandler();

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    public static void startCommandConsole() {
        CommandHandler commandHandler = new CommandHandler();
        DefaultHistory history = new DefaultHistory();
        LineReader reader = LineReaderBuilder.builder()
                .history(history)
                .build();

        // Start a new thread to read input from the console

        CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String command = reader.readLine("> ");
                log.info(commandHandler.handleCommand(command));
            }
        }, commandHandler.executorService);

    }

    public String handleCommand(String command) {
        // 解析指令
        String[] parts = command.split(" ");
        String action = parts[0].toLowerCase(Locale.ROOT);
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        if (action.startsWith("/")) {
            action = action.substring(1);
        }

        // 执行相应的操作
        return switch (action) {
            case "test" -> test(args);
            case "reload" -> reloadConfig();
            case "cleanchathistory" -> clean();
            case "gc" -> {
                System.gc();
                yield "Garbage collection triggered.";
            }
            case "help" -> "Available commands: test, reload, help, stop, cleanChatHistory, gc";
            case "stop" -> {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        executorService.shutdown();
                        System.exit(0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                yield "Stopping Telegram OneBot Adapter...";
            }
            default -> "Unknown command: " + action;
        };
    }

    private String clean() {
        List<Message> messages = HibernateFactory.selectList(Message.class);
        new Thread(() -> {
            AtomicBoolean deleteSuccess = new AtomicBoolean(true);
            messages.forEach(m -> deleteSuccess.set(HibernateFactory.delete(m)));
            messages.forEach(m -> deleteSuccess.set(HibernateFactory.delete(m)));
            if (deleteSuccess.get()) {
                log.info("Chat history cleaned successfully.");
                LogWebSocketHandler.broadcast("Chat history cleaned successfully.");
            } else {
                log.error("Failed to clean chat history.");
                LogWebSocketHandler.broadcast("Failed to clean chat history.");
            }
        }).start();


        return "Cleaning chat history, total messages: " + messages.size() + ", please wait...";
    }

    private String reloadConfig() {
        TelegramOnebotAdapter.config = ConfigGenerator.loadConfig();
        return TelegramOnebotAdapter.config.getCommand().getCommandMap().toString();
    }

    private String test(String[] args) {
        if (args.length > 0) {
            return "Hello, " + String.join(" ", args) + "!";
        } else {
            return "Hello!";
        }
    }
}