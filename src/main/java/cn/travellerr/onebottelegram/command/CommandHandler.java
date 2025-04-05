package cn.travellerr.onebottelegram.command;

import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.config.ConfigGenerator;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandHandler {

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
                commandHandler.handleCommand(command);
            }
        }, commandHandler.executorService);

    }

    public void handleCommand(String command) {
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
                test(args);
                break;
            case "reload":
                reloadConfig();
                break;
            case "help":
                System.out.println("Available commands: test, reload, help, stop");
                break;
            case "stop":
                log.info("Stopping Telegram OneBot Adapter...");
                executorService.shutdown();
                System.exit(0);
                break;
            default:
                log.error("Unknown command: " + action);
        }
    }

    private void reloadConfig() {
        TelegramOnebotAdapter.config = ConfigGenerator.loadConfig();
        System.out.println(TelegramOnebotAdapter.config.getCommand().getCommandMap());
    }

    private void test(String[] args) {
        if (args.length > 0) {
            System.out.println("Hello, " + String.join(" ", args) + "!");
        } else {
            System.out.println("Hello!");
        }
    }
}