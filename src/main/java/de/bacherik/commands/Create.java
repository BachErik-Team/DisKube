package de.bacherik.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Create {
    public static void command(SlashCommandInteractionEvent event) {
        event.reply("Coming (maybe) never").setEphemeral(true).queue();
    }
}
