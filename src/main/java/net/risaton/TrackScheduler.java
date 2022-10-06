package net.risaton;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class TrackScheduler implements AudioLoadResultHandler {
    private final AudioPlayer player;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
    }

    /**
     * Called when the requested item is a track and it was successfully loaded.
     *
     * @param track The loaded track
     */
    @Override
    public void trackLoaded(AudioTrack track) {
        player.playTrack(track);
        player.setVolume(2);
    }

    /**
     * Called when the requested item is a playlist and it was successfully loaded.
     *
     * @param playlist The loaded playlist
     */
    @Override
    public void playlistLoaded(AudioPlaylist playlist) {

    }

    /**
     * Called when there were no items found by the specified identifier.
     */
    @Override
    public void noMatches() {

    }

    /**
     * Called when loading an item failed with an exception.
     *
     * @param exception The exception that was thrown
     */
    @Override
    public void loadFailed(FriendlyException exception) {

    }
}
