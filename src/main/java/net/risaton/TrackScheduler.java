package net.risaton;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;

public class TrackScheduler implements AudioLoadResultHandler {
    private final AudioPlayer player;
    private MessageChannel messageChannel;

    public TrackScheduler(AudioPlayer player, MessageChannel messageChannel) {
        this.player = player;
        this.messageChannel = messageChannel;
    }

    /**
     * Called when the requested item is a track and it was successfully loaded.
     *
     * @param track The loaded track
     */
    @Override
    public void trackLoaded(AudioTrack track) {
        player.playTrack(track);
        player.setVolume(3);

    }

    /**
     * Called when the requested item is a playlist and it was successfully loaded.
     *
     * @param playlist The loaded playlist
     */
    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        for (AudioTrack x : playlist.getTracks()){
            player.playTrack(x);
            player.setVolume(3);
            messageChannel.createMessage(EmbedCreateSpec.builder()
                    .description(x.getInfo().title).build()).block();
//            messageChannel.createMessage().withEmbeds(x.getInfo().title).block();
            break;
        }
    }

    /**
     * Called when there were no items found by the specified identifier.
     */
    @Override
    public void noMatches() {
        messageChannel.createMessage(EmbedCreateSpec.builder()
                .description("読み込み失敗.").build()).block();

    }

    /**
     * Called when loading an item failed with an exception.
     *
     * @param exception The exception that was thrown
     */
    @Override
    public void loadFailed(FriendlyException exception) {
        messageChannel.createMessage(EmbedCreateSpec.builder()
                .description("読み込み失敗.").build()).block();
    }
}
