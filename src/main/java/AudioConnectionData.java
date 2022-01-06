import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.event.message.MessageCreateEvent;

public class AudioConnectionData {
    protected AudioConnection audioConnection;
    protected AudioPlayerManager audioPlayerManager;
    protected AudioPlayer audioPlayer;
    protected AudioSource audioSource;
    protected AudioLoadResultHandler audioLoadResultHandler;

    public AudioConnectionData(AudioConnection audioConnection, MessageCreateEvent event) {
        this.audioConnection = audioConnection;

        audioPlayerManager = new DefaultAudioPlayerManager();
        audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
        audioPlayer = audioPlayerManager.createPlayer();

        audioSource = new LavaPlayerAudioSource(event.getApi(), audioPlayer);
        this.audioConnection.setAudioSource(audioSource);

        this.audioLoadResultHandler = new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                audioPlayer.playTrack(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                for (AudioTrack track : audioPlaylist.getTracks())
                    audioPlayer.playTrack(track);
            }

            @Override
            public void noMatches() {
                event.getChannel().sendMessage("error: no messages found");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.getChannel().sendMessage("error: unable to load track");
            }
        };
    }

    public void loadTrack(String link) {
        audioPlayerManager.loadItem(link, audioLoadResultHandler);
    }
}
