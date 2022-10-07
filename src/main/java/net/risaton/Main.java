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
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import discord4j.voice.AudioProvider;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class Main {

    public static void main(String[] args) {
//        Map<Guild, VoiceConnection> guildVoiceConnectionMap = new HashMap<>();
//        Map<Guild, AudioPlayerManager> guildAudioPlayerManagerMap = new HashMap<>();
        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        final Map<Guild, AudioPlayerManager> guildAudioPlayerManagerMap = new HashMap<>();
//    private final AudioPlayer player;
        final Map<AudioPlayerManager, AudioPlayer> guildAudioPlayerMap = new HashMap<>();
        final Map<Guild, AudioProvider> guildAudioProviderMap = new HashMap<>();
        final Map<Guild, TrackScheduler> guildTrackSchedulerMap = new HashMap<>();
        final Map<Guild, VoiceChannel> guildVoiceChannelMap = new HashMap<>();


// We will be creating LavaPlayerAudioProvider in the next step

        Dotenv dotenv = Dotenv.load();
//        final String token = args[0];
        final String token = dotenv.get("DISCORD_TOKEN");
        final DiscordClient client = DiscordClient.create(token);
        final GatewayDiscordClient gateway = client.login().block();

        long appId = gateway.getRestClient().getApplicationId().block();
        ApplicationCommandRequest joinCommandReq = ApplicationCommandRequest.builder()
                .name("join")
                .description("VCに接続.")
                .build();
        ApplicationCommandRequest playCommandReq = ApplicationCommandRequest.builder()
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
        ApplicationCommandRequest stopCommandReq = ApplicationCommandRequest.builder()
                .name("stop")
                .description("音楽を止める.")
                .build();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(appId, joinCommandReq).subscribe();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(appId, playCommandReq).subscribe();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(appId, leaveCommandReq).subscribe();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(appId, stopCommandReq).subscribe();
        gateway.getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            out.println("Bot is ready.");
            out.println(event.getSelf().getUsername());
        });
        gateway.getEventDispatcher().on(ChatInputInteractionEvent.class).subscribe(event -> {
            if (event.getCommandName().equalsIgnoreCase("join")) {
                event.reply("Connecting...").withEphemeral(Boolean.TRUE).block();
                final MessageChannel messageChannel = event.getInteraction().getChannel().block();

                //
                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .color(Color.MAGENTA)
                        .title("YukiMusicV2")
                        .description("Created by Yuki.\n" +
                                "Open source.\n" +
                                "/play [URL] で再生\n" +
                                "/stop で停止\n" +
                                "/leave で退出\n" +
                                "※たまにメンテナンスで落ちます。その際は再度 /joinにて接続をお願い致します。\n" +
                                "ソースコード及び不具合等は以下まで:\n" +
                                "https://github.com/Yuki56738/YukiMusicV2.discord")
                        .build();
                messageChannel.createMessage(embed).block();
                //
                AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
                playerManager.getConfiguration()
                        .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
                AudioSourceManagers.registerRemoteSources(playerManager);
                AudioPlayer player = playerManager.createPlayer();
                AudioProvider provider = new LavaPlayerAudioProvider(player);
                TrackScheduler scheduler = new TrackScheduler(player, event.getInteraction().getChannel().block());
                guildAudioPlayerManagerMap.put(event.getInteraction().getGuild().block(), playerManager);
                guildAudioPlayerMap.put(playerManager, player);
                guildAudioProviderMap.put(event.getInteraction().getGuild().block(), provider);
                guildTrackSchedulerMap.put(event.getInteraction().getGuild().block(), scheduler);
                //

                Member member = event.getInteraction().getMember().orElse(null);
                if (member != null) {
                    VoiceState voiceState = member.getVoiceState().block();
                    if (voiceState != null) {
                        VoiceChannel voiceChannel = voiceState.getChannel().block();
                        if (voiceChannel != null) {
                            voiceChannel.join().withProvider(provider).block();
                            guildVoiceChannelMap.put(event.getInteraction().getGuild().block(), voiceChannel);
                        }
                    }
                }
            } else if (event.getCommandName().equalsIgnoreCase("play")) {


                event.reply("Connecting...").withEphemeral(Boolean.TRUE).block();

                VoiceChannel voiceChannel = guildVoiceChannelMap.get(event.getInteraction().getGuild().block());

                //                voiceChannel.join().withProvider(provider).block();
                //
//                        Member member = event.getInteraction().getMember().orElse(null);
//                        if (member != null) {
//                            final VoiceState voiceState = member.getVoiceState().block();
//                            if (voiceState != null) {
//                                final VoiceChannel voiceChannel = voiceState.getChannel().block();
//                                if (voiceChannel != null) {
////                            if (!voiceChannel.getVoiceConnection().block().isConnected().block()) {
//                                    voiceChannel.join(spec -> {
//                                        spec.setProvider(provider);
////                                guildVoiceConnectionMap.put(event.getInteraction().getGuild().block() ,spec.asRequest().block());
//                                    }).block();
////                            }
                AudioPlayerManager playerManager = guildAudioPlayerManagerMap.get(event.getInteraction().getGuild().block());
                TrackScheduler scheduler = guildTrackSchedulerMap.get(event.getInteraction().getGuild().block());
                String opt = event.getOption("url").get().getValue().get().getRaw();
                out.println(opt);
                playerManager.loadItem(opt, scheduler);

//
//
//                                    final MessageChannel messageChannel = event.getInteraction().getChannel().block();
//
//                                    //
//                                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
//                                            .color(Color.MAGENTA)
//                                            .title("YukiMusicV2")
//                                            .description("Created by Yuki.\n" +
//                                                    "Open source.\n" +
//                                                    "/play [URL] で再生\n" +
//                                                    "/stop で停止\n" +
//                                                    "/leave で退出\n" +
//                                                    "※たまにメンテナンスで落ちます。その際は再度 /joinにて接続をお願い致します。\n" +
//                                                    "ソースコード及び不具合等は以下まで:\n" +
//                                                    "https://github.com/Yuki56738/YukiMusicV2.discord")
//                                            .build();
//                                    Message greetingmsg = messageChannel.createMessage(embed).delayElement(Duration.ofSeconds(5)).block();
//                                    greetingmsg.delete().block();
                //
//                            messageChannel.createMessage(embed).block();


//                            greetingmsg.delete().block();

            } else if (event.getCommandName().equalsIgnoreCase("leave")) {
                event.reply("Disconnecting...").withEphemeral(Boolean.TRUE).block();
//                guildVoiceConnectionMap.get(event.getInteraction().getGuild().block()).disconnect().block();
                final Member member = event.getInteraction().getMember().orElse(null);
                final VoiceState voiceState = member.getVoiceState().block();
                final VoiceChannel voiceChannel = voiceState.getChannel().block();
//                voiceChannel.sendDisconnectVoiceState().block();
                voiceChannel.getVoiceConnection().block().disconnect().block();
            } else if (event.getCommandName().equalsIgnoreCase("stop")) {
                final Member member = event.getInteraction().getMember().orElse(null);
                final VoiceState voiceState = member.getVoiceState().block();
                final VoiceChannel voiceChannel = voiceState.getChannel().block();
                event.reply("Stopping...").withEphemeral(Boolean.TRUE).block();
//                player.destroy();

                AudioPlayerManager playerManager = guildAudioPlayerManagerMap.get(event.getInteraction().getGuild().block());
                AudioPlayer player = guildAudioPlayerMap.get(playerManager);
                TrackScheduler scheduler = guildTrackSchedulerMap.get(event.getInteraction().getGuild().block());
//                AudioProvider provider = guildAudioProviderMap.get(event.getInteraction().getGuild().block());
//                player.stopTrack();
//                playerManager
                player.stopTrack();
//                playerManager.shutdown();

                //
//                playerManager = new DefaultAudioPlayerManager();
//                playerManager.getConfiguration()
//                        .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
//                AudioSourceManagers.registerRemoteSources(playerManager);
//                player = playerManager.createPlayer();
//                AudioProvider provider = new LavaPlayerAudioProvider(player);
//                scheduler = new TrackScheduler(player, event.getInteraction().getChannel().block());
//                guildAudioPlayerManagerMap.put(event.getInteraction().getGuild().block(), playerManager);
//                guildAudioPlayerMap.put(playerManager, player);
//                guildAudioProviderMap.put(event.getInteraction().getGuild().block(), provider);
//                guildTrackSchedulerMap.put(event.getInteraction3().getGuild().block(), scheduler);


//                voiceChannel.getVoiceConnection().block().reconnect().block();
//                playerManager.getConfiguration()
//                        .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
//                AudioSourceManagers.registerRemoteSources(playerManager);
//                guildAudioPlayerMap.put(playerManager, player);
//                guildAudioPlayerManagerMap.put(event.getInteraction().getGuild().block(), playerManager);

            }
        });
        gateway.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            final String msg = event.getMessage().getContent();
            if (msg.equalsIgnoreCase(".debug")) {
                out.println(".debug hit.");
//                out.println(String.format("Now connected to: %s", event.getGuild().block().getName()));
                out.println("Now connected to: ");
                for (Guild x : gateway.getSelf().block().getClient().getGuilds().toIterable()) {
                    out.println(x.getName());
                }
                //                for (Object x : Flux.just(playerManager.getConfiguration()).toStream().toArray()){
//                    out.println(x);
//                }
//                out.println(String.format());
//                for (Object x : Flux.just(playerManager.getConfiguration()).toStream().toArray()){
//                    out.println(x.toString());
//                };
            }
        });
        gateway.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event -> {

            if (event.isLeaveEvent()) {
//                System.out.println(event.getShardInfo().getCount());
                if (event.getShardInfo().getCount() == 1) {
//                    event.getCurrent().getChannel().block().sendDisconnectVoiceState().block();
//                    event.getOld().get().getChannel().block().sendDisconnectVoiceState().block();
//                    guildVoiceConnectionMap.get(event.getCurrent().getGuild().block()).disconnect().block();
                    event.getOld().get().getChannel().block().getVoiceConnection().block().disconnect().block();
                    event.getCurrent().getChannel().block().getVoiceConnection().block().disconnect().block();
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