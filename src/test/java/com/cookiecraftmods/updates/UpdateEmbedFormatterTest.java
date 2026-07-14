package com.cookiecraftmods.updates;

import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateEmbedFormatterTest {
    private final UpdateEmbedFormatter formatter = new UpdateEmbedFormatter();

    @Test
    void createsRolePingBrandEmbedAndRequestedLinks() {
        UpdateAnnouncement update = announcement("mdm", "- Added a very comfy chair");

        List<MessageCreateData> messages = formatter.format(update);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getContent().contains("<@&1277223801205297306>"));
        assertTrue(messages.get(0).getContent().contains("<@&1277223877151555595>"));
        assertEquals(UpdateEmbedFormatter.BRAND_COLOR, messages.get(0).getEmbeds().get(0).getColorRaw());
        assertEquals(3, messages.get(0).getComponents().get(0).asActionRow().getComponents().size());
    }

    @Test
    void splitsLongChangelogIntoContinuationEmbeds() {
        UpdateAnnouncement update = announcement("mta", "A detailed change.\n".repeat(700));

        List<MessageCreateData> messages = formatter.format(update);

        assertTrue(messages.size() > 1);
        assertTrue(messages.get(1).getEmbeds().get(0).getTitle().contains("continued"));
        assertTrue(messages.get(0).getContent().contains("<@&1277223801205297306>"));
        assertTrue(messages.get(0).getContent().contains("<@&1507474083342843945>"));
    }

    @Test
    void projectWithoutMappedRoleAnnouncesWithoutAnyRoleMention() {
        MessageCreateData message = formatter.format(announcement("dd", "- Fixed sunrise timing")).get(0);

        assertTrue(message.getContent().startsWith("**Modern Decorations"));
        assertTrue(!message.getContent().contains("<@&"));
    }

    private UpdateAnnouncement announcement(String slug, String content) {
        return new UpdateAnnouncement(
                42,
                slug,
                "Modern Decorations",
                "26.7.0",
                content,
                "A cozy new release packed with polished details.",
                "2026-07-12T12:00:00+02:00",
                "https://cookiecraftmods.com/mods/" + slug + "/changelog",
                "https://cookiecraftmods.com/mods/" + slug + "/download",
                "https://ko-fi.com/cookiecraftmods",
                "https://cookiecraftmods.com/images/ccm-banner.webp",
                "",
                List.of()
        );
    }
}
