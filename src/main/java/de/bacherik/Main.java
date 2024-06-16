package de.bacherik;

import de.bacherik.events.SlashCommandListener;
import de.bacherik.utils.PagingHelper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -jar DisKube.jar <Discord Bot Token> <Discord Webhook>");
            System.exit(1);
        }

        String token = args[0];
        String webhook = args[1];

        ApiClient client;
        try {
            client = ClientBuilder.cluster().build();
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            System.err.println("Failed to create Kubernetes client: " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            JDA jda = JDABuilder.createDefault(token)
                    .addEventListeners(new SlashCommandListener())
                    .addEventListeners(new PagingHelper())
                    .build();

            CommandListUpdateAction commands = jda.updateCommands();
            commands.addCommands(
                    Commands.slash("ping", "Pong!"),
                    Commands.slash("create", "Create kubernetes cluster"),
                    Commands.slash("getpods", "Get pods")
                            .addOptions(
                                    new OptionData(OptionType.STRING, "namespace", "The namespace to get pods from (optional)", false)
                            )
            );
            commands.queue();
            System.out.println("Bot is up and running!");
        } catch (Exception e) {
            System.err.println("Failed to initialize JDA: " + e.getMessage());
            System.exit(1);
        }
    }
}
