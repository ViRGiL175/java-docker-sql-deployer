package ru.virgil7;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

class ConsoleStreamListener implements Runnable {

    private final InputStream onNewInput;
    private final Consumer<String> consumer;

    public ConsoleStreamListener(InputStream inputStream, Consumer<String> onNewInput) {
        this.onNewInput = inputStream;
        this.consumer = onNewInput;
    }

    @Override
    public void run() {
        new BufferedReader(new InputStreamReader(onNewInput)).lines()
                .forEach(consumer);
    }
}
