package ru.virgil7;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.virgil7.CommandsExecutor.executeCommand;

public class Main {

    private static Terminal terminal;
    private static MultiWindowTextGUI multiWindowTextGUI;
    private static TextBox nameTextBox;
    private static TextBox passTextBox;
    private static int freePort;
    private static List<Path> sqlFiles;
    private static Button button;
    private static Label hintLabel;

    public static void main(String[] args) throws IOException {

        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        sqlFiles = getSqlFiles(Paths.get("./deploy"));
        System.out.println("Found SQL files " + sqlFiles);

        terminal = new DefaultTerminalFactory().createTerminal();
        terminal.enterPrivateMode();

        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        new EmptySpace().addTo(mainPanel);
        new Label("Введите свою фамилию или ник латиницей:").addTo(mainPanel);
        nameTextBox = new TextBox("")
                .setPreferredSize(new TerminalSize(60, 1))
                .setValidationPattern(Pattern.compile("^[a-zA-Z0-9_.-]*$"))
                .addTo(mainPanel);
        new EmptySpace().addTo(mainPanel);
        new Label("Задайте пароль для контейнера:").addTo(mainPanel);
        passTextBox = new TextBox("")
                .setPreferredSize(new TerminalSize(60, 1))
                .setValidationPattern(Pattern.compile("^[a-zA-Z0-9_.-]*$"))
                .addTo(mainPanel);
        new EmptySpace().addTo(mainPanel);
        button = new Button("Создать контейнер", Main::onPress).addTo(mainPanel);
        new EmptySpace().addTo(mainPanel);
        hintLabel = new Label("Заполните данные и нажмите на кнопку выше с помощью Enter. \nПереключение между полями на TAB")
                .addTo(mainPanel);

        BasicWindow window = new BasicWindow();
        window.setTitle("Создание нового контейнера с базой данных");
        window.setComponent(mainPanel);

        multiWindowTextGUI = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
        multiWindowTextGUI.addWindowAndWait(window);
    }

    private static void deployScripts() {
        sqlFiles.forEach(path -> {
            try {
                executeCommand("docker cp " + path + " " + nameTextBox.getText() + ":" + path.getFileName());
            } catch (IOException | InterruptedException e) {
                MessageDialog.showMessageDialog(multiWindowTextGUI, "ОШИБКА РАЗМЕЩЕНИЯ КРИПТОВ", e.getLocalizedMessage());
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        sqlFiles.forEach(path -> {
            try {
                executeCommand("docker exec -u postgres " + nameTextBox.getText() + " psql -U postgres -f " + path.getFileName());
            } catch (IOException | InterruptedException e) {
                MessageDialog.showMessageDialog(multiWindowTextGUI, "ОШИБКА ЗАПОЛНЕНИЯ БД", e.getLocalizedMessage());
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void onPress() {
        FileInputStream fileInputStream = null;
        ConnectionData connectionData = null;
        try {
            fileInputStream = new FileInputStream(ConnectionData.class.getSimpleName());
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            connectionData = (ConnectionData) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (connectionData == null) {
            connectionData = new ConnectionData(new HashSet<>(), new HashSet<>());
        }
        if (nameTextBox.getText().isEmpty() || passTextBox.getText().isEmpty()) {
            MessageDialog.showMessageDialog(multiWindowTextGUI, "НЕВЕРНЫЕ ДАННЫЕ", "Проверьте введенные имя и пароль!");
        } else if (connectionData.names.contains(nameTextBox.getText())) {
            MessageDialog.showMessageDialog(multiWindowTextGUI, "НЕВЕРНЫЕ ДАННЫЕ", "Такое имя уже есть!\n\nИли было.");
        } else {
            connectionData.names.add(nameTextBox.getText());
            passTextBox.setEnabled(false);
            nameTextBox.setEnabled(false);
            button.setEnabled(false);
            hintLabel.setText("База данных создается.");
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    hintLabel.setText(hintLabel.getText() + ".");
                    if (hintLabel.getText().contains(".....")) {
                        hintLabel.setText(hintLabel.getText().replace(".....", ""));
                    }
                }
            }, 1000, 1000);
            ConnectionData finalConnectionData = connectionData;
            Thread thread = new Thread(() -> {
                try {
                    freePort = PortFinder.findFreePort(finalConnectionData.ports);
                    finalConnectionData.ports.add(freePort);
                    FileOutputStream fileOutputStream = new FileOutputStream(ConnectionData.class.getSimpleName());
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(finalConnectionData);
                    objectOutputStream.close();
                    fileOutputStream.close();
                    String command = "docker run --name " + nameTextBox.getText() + " -p " + freePort +
                            ":5432 -e POSTGRES_PASSWORD=" + passTextBox.getText() + " -e POSTGRES_USER=postgres -d postgres";
                    executeCommand(command);
                    deployScripts();
                    hintLabel.setText("База данных создана. \nКонтейнер запущен и отображается в Docker Dashboard.");
                    MessageDialog.showMessageDialog(multiWindowTextGUI, "УСПЕХ",
                            "Информация для подключения:\n" +
                                    "Имя контейнера: " + nameTextBox.getText() + "\n" +
                                    "IP-адрес: 127.0.0.1" + "\n" +
                                    "Порт: " + freePort + "\n" +
                                    "Пользователь: postgres" + "\n" +
                                    "Пароль: " + passTextBox.getText() + "\n" + "\n" +
                                    "Запомните или запишите свой пароль от БД!"
                    );
                } catch (IOException | InterruptedException e) {
                    MessageDialog.showMessageDialog(multiWindowTextGUI, "ОШИБКА ГЕНЕРАЦИИ КОНТЕЙНЕРА", e.getLocalizedMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Thread.currentThread().interrupt();
                    timer.cancel();
                }
            });
            thread.start();
        }
    }

    public static List<Path> getSqlFiles(Path dir) throws IOException {
        return Files.list(dir)
                .sorted()
                .collect(Collectors.toList());
    }

    static class ConnectionData implements Serializable {

        public Set<String> names;
        public Set<Integer> ports;

        public ConnectionData(Set<String> names, Set<Integer> ports) {
            this.names = names;
            this.ports = ports;
        }
    }
}
