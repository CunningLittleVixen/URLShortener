package io.github.cunninglittlevixen.urlshortener.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

public final class ConsoleApp {

    private final CommandProcessor processor;
    private final BufferedReader in;
    private final PrintStream out;

    public ConsoleApp(CommandProcessor processor, BufferedReader in, PrintStream out) {
        this.processor = processor;
        this.in = in;
        this.out = out;
    }

    public void run() {
        out.println(Messages.WELCOME);
        while (true) {
            out.print("> ");
            out.flush();
            String line = readLine();
            if (line == null) {
                out.println();
                out.println("Ввод завершён. До свидания!");
                return;
            }
            if (!handle(line)) {
                return;
            }
        }
    }

    private boolean handle(String line) {
        try {
            boolean goOn = processor.execute(line);
            if (goOn) {
                processor.showPendingNotifications();
            }
            return goOn;
        } catch (RuntimeException e) {
            out.println("Внутренняя ошибка: " + e.getMessage()
                + ". Если она повторяется, проверьте доступ к папке с данными и сообщите разработчику.");
            return true;
        }
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
