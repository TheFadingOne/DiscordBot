import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Function;

public class Main {
    private static String filename = "ressources/bot-token";
    private static boolean disconnect = false;

    private static HashMap<String, Function<MessageCreateEvent, Void>> commands;

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length > 0) {
            processOpts(args);
        }

        setupCommands();
        String token = readBotToken(filename);
        DiscordApi api = getDiscordApi(token);
        api.addMessageCreateListener(Main::listener);

        Scanner in = new Scanner(System.in);
        in.nextLine();
        api.disconnect();
    }

    private static void processOpts(String[] args) {
        Arrays.stream(args).forEach(s -> {
            if (s.equals("-d"))
                disconnect = true;
            else
                filename = s;
        });
    }

    private static String readBotToken(String filename) throws FileNotFoundException {
        File f = new File(filename);
        Scanner scan = new Scanner(f);
        String token = scan.nextLine();
        scan.close();
        return token;
    }

    private static DiscordApi getDiscordApi(String token) {
        return new DiscordApiBuilder().setToken(token).login().join();
    }

    private static void listener(MessageCreateEvent event) {
        MessageAuthor author = event.getMessage().getAuthor();
        if (author.isBotUser())
            return;

        String msg = event.getMessageContent();
        if (msg.charAt(0) == '!') {
            int i = msg.indexOf(' ');

            String command;
            if (i > 0)
                command = msg.substring(1, i);
            else
                command = msg.substring(1);

            commands.getOrDefault(command, Commands::noCmd).apply(event);
        }
    }

    private static void setupCommands() {
        commands = new HashMap<>();
        commands.put("hey", Commands::hey);
        commands.put("rand", Commands::rand);
        commands.put("id", Commands::id);
        commands.put("play", Commands::play);
        commands.put("playNext", Commands::playNext);
        commands.put("skip", Commands::skip);
        commands.put("queue", Commands::queue);
    }
}
