package com.cookiecraftmods.updates;

import com.cookiecraftmods.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateAnnouncementPoller {
    private final JDA jda;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final UpdateDeliveryRepository deliveries = new UpdateDeliveryRepository();
    private final UpdateEmbedFormatter formatter = new UpdateEmbedFormatter();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "cookiebot-update-poller");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean polling = new AtomicBoolean(false);

    public UpdateAnnouncementPoller(JDA jda) {
        this.jda = jda;
    }

    public void start() {
        if (isBlank(Config.getUpdateFeedSecret())) {
            System.err.println("[CookieBot] DISCORD_UPDATE_FEED_SECRET is missing; automatic update pings are disabled.");
            return;
        }

        scheduler.scheduleWithFixedDelay(this::pollSafely, 0, Config.getUpdatePollSeconds(), TimeUnit.SECONDS);
        System.out.println("[CookieBot] Automatic website update pings are enabled.");
    }

    private void pollSafely() {
        if (!polling.compareAndSet(false, true)) return;
        try {
            for (UpdateAnnouncement announcement : fetchPending()) deliver(announcement);
        } catch (Exception exception) {
            System.err.println("[CookieBot] Update poll failed: " + exception.getMessage());
        } finally {
            polling.set(false);
        }
    }

    private List<UpdateAnnouncement> fetchPending() throws Exception {
        HttpRequest request = requestBuilder(Config.getUpdateFeedUrl()).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccess(response, "fetch pending updates");

        DataArray data = DataObject.fromJson(response.body()).getArray("announcements");
        List<UpdateAnnouncement> announcements = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) announcements.add(UpdateAnnouncement.fromData(data.getObject(i)));
        return announcements;
    }

    private void deliver(UpdateAnnouncement announcement) throws Exception {
        UpdateDeliveryRepository.DeliveryProgress progress = deliveries.get(announcement.id());
        if (progress.completed()) {
            acknowledge(announcement.id(), progress.messageIds());
            return;
        }

        TextChannel channel = jda.getTextChannelById(Config.getUpdatesChannelId());
        if (channel == null) throw new IllegalStateException("Updates channel " + Config.getUpdatesChannelId() + " was not found");

        List<MessageCreateData> parts = formatter.format(announcement);
        List<String> messageIds = new ArrayList<>(progress.messageIds());

        for (int i = progress.nextPart(); i < parts.size(); i++) {
            Message sent = channel.sendMessage(parts.get(i))
                    .setNonce("cookiecraft-update-" + announcement.id() + "-" + i)
                    .complete();
            messageIds.add(sent.getId());
            deliveries.recordPart(announcement.id(), i + 1, messageIds);
        }

        deliveries.markComplete(announcement.id(), parts.size(), messageIds);
        acknowledge(announcement.id(), messageIds);
        System.out.println("[CookieBot] Published website update #" + announcement.id() + " for " + announcement.slug());
    }

    private void acknowledge(long announcementId, List<String> messageIds) throws Exception {
        DataObject body = DataObject.empty().put("message_ids", DataArray.fromCollection(messageIds));
        String endpoint = trimTrailingSlash(Config.getUpdateFeedUrl()) + "/" + announcementId + "/delivered";
        HttpRequest request = requestBuilder(endpoint)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toJson()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccess(response, "acknowledge update " + announcementId);
    }

    private HttpRequest.Builder requestBuilder(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + Config.getUpdateFeedSecret());
    }

    private void requireSuccess(HttpResponse<String> response, String operation) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Could not " + operation + " (HTTP " + response.statusCode() + ")");
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
