package net.risaton;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class Main {
    public static void main(String[] args) {
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