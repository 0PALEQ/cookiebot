package com.cookiecraftmods.updates;

import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.ArrayList;
import java.util.List;

public record UpdateAnnouncement(
        long id,
        String slug,
        String title,
        String version,
        String content,
        String summary,
        String publishedAt,
        String websiteUrl,
        String downloadUrl,
        String kofiUrl,
        String imageUrl,
        String thumbnailUrl,
        List<ExternalLink> externalLinks
) {
    public static UpdateAnnouncement fromData(DataObject data) {
        List<ExternalLink> links = new ArrayList<>();
        DataArray array = data.optArray("external_links").orElse(DataArray.empty());
        for (int i = 0; i < array.length(); i++) {
            DataObject link = array.getObject(i);
            links.add(new ExternalLink(link.getString("label", ""), link.getString("url", "")));
        }

        return new UpdateAnnouncement(
                data.getLong("id"),
                data.getString("slug", ""),
                data.getString("title", "A CookieCraftMods project"),
                data.getString("version", "New update"),
                data.getString("content", ""),
                data.getString("summary", ""),
                data.getString("published_at", ""),
                data.getString("website_url", ""),
                data.getString("download_url", ""),
                data.getString("kofi_url", ""),
                data.getString("image_url", ""),
                data.getString("thumbnail_url", ""),
                List.copyOf(links)
        );
    }

    public record ExternalLink(String label, String url) {}
}
