import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Commands {
    public static Void hey(MessageCreateEvent event) {
        event.getChannel().sendMessage("Fick dich ins Knie");
        return null;
    }

    public static Void rand(MessageCreateEvent event) {
        String msg = event.getMessageContent();
        String[] parts = msg.split(" ");
        double lower = 0;
        double upper = 1;
        try {
            if (parts.length > 1)
                lower = Double.parseDouble(parts[1]);
            if (parts.length > 2)
                upper = Double.parseDouble(parts[2]);
        } catch (Exception e) {
            event.getChannel().sendMessage("error: unable to parse bounds");
            return null;
        }

        if (lower < 0 || upper < 0) {
            event.getChannel().sendMessage("error: invalid bounds");
            return null;
        }

        if (lower > upper) {
            event.getChannel().sendMessage("error: invalid bounds");
        }

        double d = new Random().nextDouble();
        d *= upper - lower;
        d += lower;
        event.getChannel().sendMessage(((Integer) (int) d).toString());
        return null;
    }

    public static Void id(MessageCreateEvent event) {
        event.getChannel().sendMessage(event.getMessageContent());
        return null;
    }

    // TODO maybe this needs to be synchronized as well
    private static Optional<AudioConnectionData> audioConnectionData = Optional.empty();
    private static LinkedList<String> songs = new LinkedList<>();
    private static Semaphore songMutex = new Semaphore(1);

    public static Void play(MessageCreateEvent event) {
        Optional<ServerVoiceChannel> op = event.getMessageAuthor().getConnectedVoiceChannel();
        if (op.isEmpty()) {
            event.getChannel().sendMessage("error: you must be connected to a channel to play music");
            return null;
        }

        String[] parts = event.getMessageContent().split(" ");
        String link;
        if (parts.length > 0) {
            link = event.getMessageContent().split(" ")[1];
        } else {
            event.getChannel().sendMessage("error: no link given");
            return null;
        }

        if (audioConnectionData.isPresent()) {
            try {
                songMutex.acquire();
                songs.add(link);
                songMutex.release();
            } catch (Exception e) {
                event.getChannel().sendMessage("error: unable to queue song");
            }
        } else {
            ServerVoiceChannel channel = op.get();
            channel.connect().thenAccept(audioConnection -> {
                audioConnectionData = Optional.of(new AudioConnectionData(audioConnection, event));
                audioConnectionData.get().loadTrack(link);
                startMusicSupplyThread();
            }).exceptionally(e -> {
                System.out.println("Unable to connect to channel");
                return null;
            });
        }
        return null;
    }

    public static void startMusicSupplyThread() {
        Thread thread = new Thread(Commands::musicSupplyThread);
        thread.start();
    }

    // TODO this is jank as fuck
    // TODO busy waiting sucks. Try to create wait notify
    public static void musicSupplyThread() {
        // wait for first track to be loaded
        while (audioConnectionData.get().audioPlayer.getPlayingTrack() == null) {
            Thread.onSpinWait();
        }

        while (true) {
            if (audioConnectionData.get().audioPlayer.getPlayingTrack() != null) {
                Thread.onSpinWait();
                continue;
            }

            try {
                songMutex.acquire();

                // TODO this might be redundant
                if (audioConnectionData.get().audioPlayer.getPlayingTrack() != null) {
                    songMutex.release();
                    continue;
                }

                if (songs.isEmpty() && audioConnectionData.get().audioPlayer.getPlayingTrack() == null) {
                    songMutex.release();
                    break;
                }

                audioConnectionData.get().loadTrack(songs.poll());
                songMutex.release();

                // wait for next track to be loaded
            } catch (Exception e) {
            }

            while (audioConnectionData.get().audioPlayer.getPlayingTrack() == null) {
                Thread.onSpinWait();
            }
        }

        audioConnectionData = Optional.empty();
        System.out.println("Thread exits");
    }

    public static Void playNext(MessageCreateEvent event) {
        if (audioConnectionData.isEmpty()) {
            play(event);
            return null;
        }

        String[] parts = event.getMessageContent().split(" ");
        if (parts.length < 2) {
            event.getChannel().sendMessage("error: no link provided");
        }
        String link = parts[1];

        try {
            songMutex.acquire();

            songs.add(0, link);

            songMutex.release();
        } catch (Exception e) {}

        return null;
    }

    // TODO this sucks because it only shows links
    public static Void queue(MessageCreateEvent event) {
        String msg = event.getMessageContent();
        StringBuilder sb = new StringBuilder();

        try {
            songMutex.acquire();

            for (int i = 0; i < Math.min(10, songs.size()); i++) {
                sb.append(((Integer) (i+1)).toString() + "\t\t" + songs.get(i) + '\n');
            }
            songMutex.release();
        } catch (Exception e) {}

        EmbedBuilder embed = new EmbedBuilder()
                .addField("Queue", sb.toString());

        event.getChannel().sendMessage(embed);
        return null;
    }

    public static Void skip(MessageCreateEvent event) {
        if (audioConnectionData.isEmpty()) {
            event.getChannel().sendMessage("error: no music is playing");
            return null;
        }

        try {
            songMutex.acquire();
            audioConnectionData.get().loadTrack(songs.poll());
            songMutex.release();
        } catch (Exception e) {}

        return null;
    }

    public static Void noCmd(MessageCreateEvent event) {
        event.getChannel().sendMessage("error: no such command exists");
        return null;
    }
}
