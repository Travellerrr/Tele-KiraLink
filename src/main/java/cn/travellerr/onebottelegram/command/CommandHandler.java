package cn.travellerr.onebottelegram.command;

import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.config.ConfigGenerator;
import cn.travellerr.onebottelegram.webui.api.LogWebSocketHandler;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        String action = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        if (action.startsWith("/")) {
            action = action.substring(1);
        }

        // 执行相应的操作
        switch (action) {
            case "test":
                return test(args);
            case "reload":
                return reloadConfig();
            case "help":
                return "Available commands: test, reload, help, stop";
            case "stop":
                log.info("Stopping Telegram OneBot Adapter...");
                LogWebSocketHandler.broadcast("Stopping Telegram OneBot Adapter...");
                executorService.shutdown();
                System.exit(0);
                return "";
            default:
                return "Unknown command: " + action;
        }
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