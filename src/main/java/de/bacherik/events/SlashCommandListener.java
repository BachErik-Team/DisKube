package de.bacherik.events;

import de.bacherik.commands.Create;
import de.bacherik.commands.GetPods;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class SlashCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                event.reply("Pong!").queue();
                break;
            case "create":
                Create.command(event);
                break;
            case "getpods":
                ReplyCallbackAction reply = event.deferReply(true);
                reply.queue(
                        v -> GetPods.command(event),
                        throwable -> event.reply("Error: " + throwable.getMessage()).setEphemeral(true).queue()
                );
                break;
            default: {
                event.reply("Unknown command!").queue();
            }
        }
    }
}
