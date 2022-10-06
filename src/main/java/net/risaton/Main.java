package net.risaton;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.voice.AudioProvider;
import io.github.cdimascio.dotenv.Dotenv;

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

        Dotenv dotenv = Dotenv.load();
//        final String token = args[0];
        final String token = dotenv.get("DISCORD_TOKEN");
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
        ApplicationCommandRequest leaveCommandReq = ApplicationCommandRequest.builder()
                .name("leave")
                .description("VCから切断.")
                .build();

        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(appId, joinCommandReq).subscribe();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(appId, leaveCommandReq).subscribe();
        gateway.getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            System.out.println("Bot is ready.");
        });
        gateway.getEventDispatcher().on(ChatInputInteractionEvent.class).subscribe(event -> {
            if (event.getCommandName().equalsIgnoreCase("play")) {
                event.reply("Connecting...").block();
                final TrackScheduler scheduler = new TrackScheduler(player);
                final Member member = event.getInteraction().getMember().orElse(null);
                if (member != null) {
                    final VoiceState voiceState = member.getVoiceState().block();
                    if (voiceState != null) {
                        final VoiceChannel voiceChannel = voiceState.getChannel().block();
                        if (voiceChannel != null) {
                            voiceChannel.join(spec -> spec.setProvider(provider)).block();
                            final MessageChannel messageChannel = event.getInteraction().getChannel().block();
                            String opt = event.getOption("url").get().getValue().get().getRaw();
//                            System.out.println(opt);
                            playerManager.loadItem(opt, scheduler);
                        }
                    }
                }
            } else if (event.getCommandName().equalsIgnoreCase("leave")) {
                event.reply("Disconnecting...").block();
                final Member member = event.getInteraction().getMember().orElse(null);
                final VoiceState voiceState = member.getVoiceState().block();
                final VoiceChannel voiceChannel = voiceState.getChannel().block();
                voiceChannel.sendDisconnectVoiceState().block();
            }
        });
        gateway.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event -> {

            if (event.isLeaveEvent()){
//                System.out.println(event.getShardInfo().getCount());
                if (event.getShardInfo().getCount()  == 1 ){
//                    event.getCurrent().getChannel().block().sendDisconnectVoiceState().block();
                    event.getOld().get().getChannel().block().sendDisconnectVoiceState().block();
                }
            }
//            System.out.println(event.isLeaveEvent());
//            final Member member = event.getCurrent().getMember().block();
//            final VoiceState voiceState = member.getVoiceState().block();
//            final VoiceChannel voiceChannel = voiceState.getChannel().block();

//            System.out.println(voiceChannel.getMembers().count().block().intValue());
//            if (event.isLeaveEvent()) {


//                if (voiceChannel.getMembers().count().block().intValue() == 1) {
//                    voiceChannel.sendDisconnectVoiceState().block();
//                }
//            }
        });
        gateway.onDisconnect().block();
    }
}