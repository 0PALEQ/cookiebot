package com.cookiecraftmods.updates;

import com.cookiecraftmods.config.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UpdateEmbedFormatter {
    static final int BRAND_COLOR = 0xE7A856;
    private static final int FIRST_CHANGELOG_LIMIT = 3000;
    private static final int CONTINUATION_LIMIT = 3900;
    private static final int MAX_PARTS = 10;

    public List<MessageCreateData> format(UpdateAnnouncement update) {
        List<String> changelogParts = splitChangelog(update.content());
        int totalParts = changelogParts.size();
        List<MessageCreateData> messages = new ArrayList<>();

        EmbedBuilder first = new EmbedBuilder()
                .setColor(BRAND_COLOR)
                .setTitle(limit("Fresh from the oven: " + update.title() + " " + update.version(), 256), validUrl(update.websiteUrl()) ? update.websiteUrl() : null)
                .setDescription(firstDescription(update.summary(), changelogParts.get(0)))
                .addField("Version", safeInline(update.version()), true)
                .addField("Published", formatDate(update.publishedAt()), true)
                .addField("Ready to explore?", "Grab the build below, then tell us what you create", false)
                .setFooter("CookieCraftMods • Update #" + update.id() + " • Part 1/" + totalParts);

        if (validUrl(update.imageUrl())) first.setImage(update.imageUrl());
        if (validUrl(update.thumbnailUrl())) first.setThumbnail(update.thumbnailUrl());

        MessageCreateBuilder firstMessage = new MessageCreateBuilder()
                .setContent(firstContent(update))
                .setEmbeds(first.build())
                .setAllowedMentions(EnumSet.of(Message.MentionType.ROLE));

        String roleId = Config.getUpdateRoleId(update.slug());
        if (roleId != null) {
            String allUpdatesRoleId = Config.getAllUpdatesRoleId();
            firstMessage.mentionRoles(allUpdatesRoleId, roleId);
        }

        List<Button> buttons = linkButtons(update);
        if (!buttons.isEmpty()) firstMessage.setComponents(ActionRow.of(buttons));
        messages.add(firstMessage.build());

        for (int i = 1; i < totalParts; i++) {
            EmbedBuilder continuation = new EmbedBuilder()
                    .setColor(BRAND_COLOR)
                    .setTitle(limit("Changelog continued — " + update.title(), 256))
                    .setDescription(changelogParts.get(i))
                    .setFooter("CookieCraftMods • Update #" + update.id() + " • Part " + (i + 1) + "/" + totalParts);

            messages.add(new MessageCreateBuilder()
                    .setEmbeds(continuation.build())
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .build());
        }

        return List.copyOf(messages);
    }

    private String firstContent(UpdateAnnouncement update) {
        String roleId = Config.getUpdateRoleId(update.slug());
        String announcement = "**" + update.title() + " " + update.version() + " just landed!**";
        return roleId == null
                ? announcement
                : "<@&" + Config.getAllUpdatesRoleId() + "> <@&" + roleId + "> " + announcement;
    }

    private String firstDescription(String summary, String changelog) {
        String intro = summary == null || summary.isBlank()
                ? "The workshop doors are open and a brand-new build is ready to play with."
                : summary.trim();
        return intro + "\n\n## What changed\n" + changelog;
    }

    List<String> splitChangelog(String raw) {
        String content = raw == null || raw.isBlank() ? "No release notes were provided for this build." : raw.trim();
        List<String> parts = new ArrayList<>();
        int offset = 0;

        while (offset < content.length() && parts.size() < MAX_PARTS) {
            int limit = parts.isEmpty() ? FIRST_CHANGELOG_LIMIT : CONTINUATION_LIMIT;
            int end = Math.min(content.length(), offset + limit);
            if (end < content.length()) {
                int newline = content.lastIndexOf('\n', end);
                int space = content.lastIndexOf(' ', end);
                int boundary = newline > offset + limit / 2 ? newline : space;
                if (boundary > offset + limit / 2) end = boundary;
            }
            parts.add(content.substring(offset, end).trim());
            offset = end;
            while (offset < content.length() && Character.isWhitespace(content.charAt(offset))) offset++;
        }

        if (offset < content.length()) {
            int last = parts.size() - 1;
            String suffix = "\n\n*The changelog is extra chunky today — read the rest on the website.*";
            String value = parts.get(last);
            int keep = Math.max(0, CONTINUATION_LIMIT - suffix.length());
            parts.set(last, value.substring(0, Math.min(value.length(), keep)).trim() + suffix);
        }

        return parts;
    }

    private List<Button> linkButtons(UpdateAnnouncement update) {
        List<Button> buttons = new ArrayList<>();
        Set<String> urls = new HashSet<>();
        addButton(buttons, urls, "Read changelog", update.websiteUrl());
        addButton(buttons, urls, "Download", update.downloadUrl());
        addButton(buttons, urls, "Support on Ko-fi", update.kofiUrl());

        for (UpdateAnnouncement.ExternalLink link : update.externalLinks()) {
            if (buttons.size() >= 5) break;
            String lower = link.label().toLowerCase(Locale.ROOT);
            if (lower.contains("curseforge") || lower.contains("modrinth")) {
                addButton(buttons, urls, link.label(), link.url());
            }
        }
        return buttons;
    }

    private void addButton(List<Button> buttons, Set<String> urls, String label, String url) {
        if (!validUrl(url) || !urls.add(url)) return;
        buttons.add(Button.link(url, label));
    }

    private boolean validUrl(String value) {
        return value != null && (value.startsWith("https://") || value.startsWith("http://"));
    }

    private String safeInline(String value) {
        return value == null || value.isBlank() ? "New release" : value.substring(0, Math.min(1000, value.length()));
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private String formatDate(String value) {
        if (value == null || value.length() < 10) return "Just now";
        try {
            return LocalDate.parse(value.substring(0, 10)).format(DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH));
        } catch (DateTimeParseException ignored) {
            return value.substring(0, Math.min(1000, value.length()));
        }
    }
}
