package de.bacherik.utils;

import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PagingHelper extends ListenerAdapter {

    private static final int PAGE_SIZE = 2000; // Discord message size limit
    private final List<Container> containers = new ArrayList<>();
    private final List<String> pages = new ArrayList<>();
    private static final Map<Long, PagingHelper> helperMap = new HashMap<>();

    private long messageId;

    public void addContainer(Container container) {
        containers.add(container);
        System.out.println("Container added: " + container.getContent());
    }

    public void generatePages() {
        System.out.println("generatePages called with " + containers.size() + " containers.");
        StringBuilder currentPage = new StringBuilder();
        for (Container container : containers) {
            if (currentPage.length() + container.getLength() > PAGE_SIZE) {
                pages.add(currentPage.toString());
                System.out.println("Page created with length: " + currentPage.length());
                currentPage = new StringBuilder();
            }
            currentPage.append(container.getContent()).append("\n");
        }
        if (!currentPage.isEmpty()) {
            pages.add(currentPage.toString());
            System.out.println("Page created with length: " + currentPage.length());
        }
        System.out.println("Total pages generated: " + pages.size());
    }


    public void sendPagedResponse(SlashCommandInteractionEvent event, int pageIndex) {
        System.out.println("sendPagedResponse called with pageIndex: " + pageIndex);
        if (pages.isEmpty()) {
            System.out.println("No pages found.");
            event.getHook().editOriginal("No content found.").setActionRow(
                    Button.primary("prevPage:0:0", "Previous").asDisabled(),
                    Button.primary("pageInfo:0:0", "0/0").asDisabled(),
                    Button.primary("nextPage:0:0", "Next").asDisabled()
            ).queue();
            return;
        }

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;

        int finalPageIndex = pageIndex;
        event.getHook().editOriginal(pages.get(pageIndex))
                .setActionRow(
                        Button.primary("prevPage:temp:temp:temp", "Previous").withDisabled(pageIndex == 0),
                        Button.primary("pageInfo:temp:temp:temp", (pageIndex + 1) + "/" + pages.size()).asDisabled(),
                        Button.primary("nextPage:temp:temp:temp", "Next").withDisabled(pageIndex == pages.size() - 1)
                )
                .queue(success -> event.getHook().retrieveOriginal().queue(original -> {
                    messageId = original.getIdLong();
                    helperMap.put(messageId, this);

                    // Update buttons with the correct messageId
                    event.getHook().editOriginal(pages.get(finalPageIndex))
                            .setActionRow(
                                    Button.primary("prevPage:" + messageId + ":" + finalPageIndex + ":" + pages.size(), "Previous").withDisabled(finalPageIndex == 0),
                                    Button.primary("pageInfo:" + messageId + ":" + finalPageIndex + ":" + pages.size(), (finalPageIndex + 1) + "/" + pages.size()).asDisabled(),
                                    Button.primary("nextPage:" + messageId + ":" + finalPageIndex + ":" + pages.size(), "Next").withDisabled(finalPageIndex == pages.size() - 1)
                            )
                            .queue();
                }));
    }

    public void sendPagedResponse(ButtonInteractionEvent event, int pageIndex) {
        System.out.println("sendPagedResponse called with pageIndex: " + pageIndex);
        if (pages.isEmpty()) {
            System.out.println("No pages found.");
            event.editMessage("No content found.").setActionRow(
                    Button.primary("prevPage:0:0", "Previous").asDisabled(),
                    Button.primary("pageInfo:0:0", "0/0").asDisabled(),
                    Button.primary("nextPage:0:0", "Next").asDisabled()
            ).queue();
            return;
        }

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;

        event.editMessage(pages.get(pageIndex))
                .setActionRow(
                        Button.primary("prevPage:" + messageId + ":" + pageIndex + ":" + pages.size(), "Previous").withDisabled(pageIndex == 0),
                        Button.primary("pageInfo:" + messageId + ":" + pageIndex + ":" + pages.size(), (pageIndex + 1) + "/" + pages.size()).asDisabled(),
                        Button.primary("nextPage:" + messageId + ":" + pageIndex + ":" + pages.size(), "Next").withDisabled(pageIndex == pages.size() - 1)
                )
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] idParts = event.getComponentId().split(":");
        if (idParts.length != 4 || (!idParts[0].equals("prevPage") && !idParts[0].equals("nextPage"))) {
            return;
        }

        long messageId = Long.parseLong(idParts[1]);
        int pageIndex = Integer.parseInt(idParts[2]);
        int totalPages = Integer.parseInt(idParts[3]);

        PagingHelper helper = helperMap.get(messageId);
        if (helper == null) {
            System.out.println("No helper found for messageId: " + messageId);
            event.editMessage("No content found.").setActionRow(
                    Button.primary("prevPage:0:0", "Previous").asDisabled(),
                    Button.primary("pageInfo:0:0", "0/0").asDisabled(),
                    Button.primary("nextPage:0:0", "Next").asDisabled()
            ).queue();
            return;
        }

        if (idParts[0].equals("prevPage") && pageIndex > 0) {
            pageIndex--;
        } else if (idParts[0].equals("nextPage") && pageIndex < totalPages - 1) {
            pageIndex++;
        }

        helper.sendPagedResponse(event, pageIndex);
    }
}
