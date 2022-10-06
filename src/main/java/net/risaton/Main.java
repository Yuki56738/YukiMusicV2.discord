package net.risaton;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.voice.AudioProvider;

public class Main {

    public static void main(String[] args) {
        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

// This is an optimization strategy that Discord4J can utilize.
// It is not important to understand
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

// Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);

// Create an AudioPlayer so Discord4J can receive audio data
        final AudioPlayer player = playerManager.createPlayer();

// We will be creating LavaPlayerAudioProvider in the next step
        AudioProvider provider = new LavaPlayerAudioProvider(player);

        final String token = args[0];
        final DiscordClient client = DiscordClient.create(token);
        final GatewayDiscordClient gateway = client.login().block();

        long appId = gateway.getRestClient().getApplicationId().block();
        ApplicationCommandRequest joinCommandReq = ApplicationCommandRequest.builder()
                .name("play")
                .description("音楽を再生する.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("url")
                        .description("URL")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(appId, joinCommandReq).subscribe();
        gateway.on(ReadyEvent.class).subscribe(event -> {
            System.out.println("Bot is ready.");
        });
        gateway.onDisconnect().block();
    }
}