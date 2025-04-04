package cn.travellerr.onebottelegram.command;

import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.config.ConfigGenerator;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandHandler {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void startCommandConsole() {
        CommandHandler commandHandler = new CommandHandler();
        Scanner scanner = new Scanner(System.in);

        // Start a new thread to read input from the console
        new Thread(() -> {
            while (true) {
                String command = scanner.nextLine();
                commandHandler.handleCommand(command);
            }
        }).start();
    }

    public void handleCommand(String command) {
        CompletableFuture.runAsync(() -> {
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
                case "stop":
                    System.out.println("Stopping the application...");
                    executorService.shutdown();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command: " + action);
            }
        }, executorService);
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