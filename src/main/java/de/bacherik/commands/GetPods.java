package de.bacherik.commands;

import de.bacherik.utils.PagingHelper;
import de.bacherik.utils.Container;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GetPods {

    public static void command(SlashCommandInteractionEvent event) {
        CoreV1Api api = new CoreV1Api();

        Optional<String> requestedNamespace = Optional.ofNullable(event.getOption("namespace"))
                .map(OptionMapping::getAsString);

        try {
            List<V1Pod> pods;
            if (requestedNamespace.isPresent()) {
                String namespace = requestedNamespace.get();
                if (!namespaceExists(api, namespace)) {
                    event.getHook().editOriginal("Namespace `" + namespace + "` does not exist.\n").queue();
                } else {
                    pods = listPodsInNamespace(api, namespace);
                    sendPagedResponse(event, pods);
                }
            } else {
                pods = listPodsInAllNamespaces(api);
                sendPagedResponse(event, pods);
            }
        } catch (ApiException e) {
            event.getHook().editOriginal("Error while fetching pods: " + e.getMessage() + "\n").queue();
        }
    }

    private static boolean namespaceExists(CoreV1Api api, String namespace) throws ApiException {
        V1NamespaceList list = api.listNamespace().execute();
        return list.getItems().stream()
                .anyMatch(ns -> namespace.equals(Objects.requireNonNull(ns.getMetadata()).getName()));
    }

    private static List<V1Pod> listPodsInNamespace(CoreV1Api api, String namespace) throws ApiException {
        V1PodList pods = api.listNamespacedPod(namespace).execute();
        return pods.getItems();
    }

    private static List<V1Pod> listPodsInAllNamespaces(CoreV1Api api) throws ApiException {
        V1PodList pods = api.listPodForAllNamespaces().execute();
        return pods.getItems();
    }

    private static void sendPagedResponse(SlashCommandInteractionEvent event, List<V1Pod> pods) {
        if (pods.isEmpty()) {
            event.getHook().editOriginal("No pods found.").queue();
            return;
        }

        PagingHelper pagingHelper = new PagingHelper();
        for (V1Pod pod : pods) {
            pagingHelper.addContainer(new Container(formatPodInfo(pod)));
        }
        System.out.println("Containers added: " + pods.size());
        pagingHelper.generatePages();

        pagingHelper.sendPagedResponse(event, 0);
    }



    private static String formatPodInfo(V1Pod pod) {
        StringBuilder info = new StringBuilder();
        Optional.ofNullable(pod.getMetadata())
                .ifPresent(metadata -> {
                    String name = Optional.ofNullable(metadata.getName()).orElse("Unknown");
                    String phase = Optional.ofNullable(pod.getStatus())
                            .map(status -> getEmojiForPhase(Objects.requireNonNull(status.getPhase())) + " " + status.getPhase())
                            .orElse("Unknown Phase");
                    String creationTimestamp = Optional.ofNullable(metadata.getCreationTimestamp())
                            .map(OffsetDateTime::toString)
                            .orElse("Unknown Creation Time");
                    String podIP = Optional.ofNullable(pod.getStatus())
                            .map(V1PodStatus::getPodIP)
                            .orElse("Unknown IP");

                    info.append(name)
                            .append(" - ")
                            .append(phase)
                            .append("\nCreation Time: ").append(creationTimestamp)
                            .append("\nIP: ").append(podIP)
                            .append("\n\n");
                });
        return info.toString();
    }

    private static String getEmojiForPhase(String phase) {
        return switch (phase) {
            case "Pending" -> "⏳";
            case "Running" -> "✅";
            case "Succeeded" -> "🎉";
            case "Failed" -> "❌";
            default -> "❓";
        };
    }
}
