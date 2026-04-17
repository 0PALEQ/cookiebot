package com.cookiecraftmods.utils;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public final class EmbedUtil {
    private EmbedUtil() {}

    public static EmbedBuilder defaultEmbed() {
        return new EmbedBuilder()
                .setColor(new Color(0x5865F2));
    }
}
