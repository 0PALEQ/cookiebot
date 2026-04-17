package com.cookiecraftmods.events;

import com.cookiecraftmods.database.UserRepository;
import com.cookiecraftmods.database.UserRepository.UserProfile;
import com.cookiecraftmods.database.DatabaseManager;
import com.cookiecraftmods.utils.EmbedUtil;
import com.cookiecraftmods.utils.LevelUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlashCommandListener extends ListenerAdapter {
    private static final int DAILY_COOKIES = 10;
    private final UserRepository repo = UserRepository.getInstance();
    private final DatabaseManager db = DatabaseManager.getInstance();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                handlePing(event);
                break;
            case "profile":
                var optUser = event.getOption("user");
                var user = optUser != null ? optUser.getAsUser() : event.getUser();
                long uid = user.getIdLong();
                String uname = user.getName();
                repo.ensureUser(uid, uname);
                UserProfile p = repo.getProfile(uid);
                if (p == null) {
                    event.reply("Profile not found.").setEphemeral(true).queue();
                    return;
                }
                int lvl = LevelUtil.levelForXp(p.xp);
                int into = LevelUtil.xpIntoLevel(p.xp);
                int need = LevelUtil.xpForLevel(lvl + 1) - LevelUtil.xpForLevel(lvl);
                String bar = LevelUtil.progressBar(10, into, need);
                EmbedBuilder eb = EmbedUtil.defaultEmbed()
                        .setTitle(user.getName() + "'s Profile")
                        .addField("XP", String.valueOf(p.xp), true)
                        .addField("Cookies", String.valueOf(p.cookies), true)
                        .addField("Reputation", String.valueOf(p.reputation), true)
                        .addField("Level", String.valueOf(lvl), true)
                        .addField("Progress", "`" + into + "/" + need + " XP to L" + (lvl + 1) + "`\n```\n" + bar + "\n```", false);
                event.replyEmbeds(eb.build()).queue();
                break;
            case "leaderboard":
                handleLeaderboard(event);
                break;
            case "help":
                handleHelp(event);
                break;
            case "rep":
                var target = event.getOption("user") != null ? event.getOption("user").getAsUser() : null;
                if (target == null) {
                    event.reply("You must specify a user.").setEphemeral(true).queue();
                    return;
                }
                long giver = event.getUser().getIdLong();
                long receiver = target.getIdLong();
                repo.ensureUser(giver, event.getUser().getName());
                repo.ensureUser(receiver, target.getName());
                boolean ok = repo.tryGiveRep(giver, receiver);
                if (!ok) {
                    event.reply("You have already given a reputation point today, or invalid target.").setEphemeral(true).queue();
                } else {
                    event.reply("You gave a reputation point to " + target.getAsTag() + "!").queue();
                }
                break;
            case "daily":
                long uid2 = event.getUser().getIdLong();
                repo.ensureUser(uid2, event.getUser().getName());
                if (!repo.canClaimDaily(uid2)) {
                    event.reply("You already claimed your daily reward. Try again later.").setEphemeral(true).queue();
                    return;
                }
                boolean claimed = repo.claimDaily(uid2, DAILY_COOKIES);
                if (claimed) {
                    event.reply("You claimed your daily reward: " + DAILY_COOKIES + " cookies!").queue();
                } else {
                    event.reply("Failed to claim reward. Please try again later.").setEphemeral(true).queue();
                }
                break;
        }
    }

    private void handlePing(@NotNull SlashCommandInteractionEvent event) {
        // Permission: Manage Server (Guild)
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.reply("You don't have permission!").setEphemeral(true).queue();
            return;
        }

        String type = event.getOption("type") != null ? event.getOption("type").getAsString() : null;
        String content = event.getOption("content") != null ? event.getOption("content").getAsString() : "";
        String mod = event.getOption("mod") != null ? event.getOption("mod").getAsString() : null;
        String version = event.getOption("version") != null ? event.getOption("version").getAsString() : null;
        String date = event.getOption("date") != null ? event.getOption("date").getAsString() : null;
        String pings = event.getOption("pings") != null ? event.getOption("pings").getAsString() : "";

        if (type == null) {
            event.reply("Missing required option: type").setEphemeral(true).queue();
            return;
        }

        if ("announcement".equalsIgnoreCase(type)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📢 Announcement")
                    .setDescription(content == null ? "" : content)
                    .setColor(0x1F8B4C)
                    .setFooter("CookieCraft Mods", null);

            event.reply(pings)
                    .addEmbeds(embed.build())
                    .queue();
            return;
        }

        if ("update".equalsIgnoreCase(type)) {
            if (isBlank(mod) || isBlank(version) || isBlank(date)) {
                event.reply("For update pings, specify mod, version, and date.").setEphemeral(true).queue();
                return;
            }

            // Query DB for mod links
            String displayName = null;
            String linksJson = null;
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT display_name, links FROM mods WHERE name = ?")) {
                ps.setString(1, mod);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        displayName = rs.getString(1);
                        linksJson = rs.getString(2);
                    }
                }
            } catch (SQLException e) {
                event.reply("Database error while fetching mod info: " + e.getMessage()).setEphemeral(true).queue();
                return;
            }

            if (displayName == null || linksJson == null) {
                event.reply("No links found for mod \"" + mod + "\"").setEphemeral(true).queue();
                return;
            }

            List<LinkDef> links = parseLinks(linksJson);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🛠 Update: " + displayName + " " + version)
                    .addField("Date", date, true)
                    .addField("Details", isBlank(content) ? "No details provided" : content, false)
                    .setColor(0x0099FF)
                    .setFooter("CookieCraft Mods", null);

            if (!links.isEmpty()) {
                StringBuilder linksText = new StringBuilder();
                for (LinkDef l : links) {
                    String icon = "";
                    String lower = l.name.toLowerCase();
                    if (lower.contains("curseforge")) icon = "🟠 ";
                    else if (lower.contains("modrinth")) icon = "🟢 ";
                    else if (lower.contains("cookiecraft")) icon = "🟡 ";
                    linksText.append(icon)
                            .append("[")
                            .append(l.name)
                            .append("](")
                            .append(l.url)
                            .append(")\n");
                }
                embed.addField("Links", linksText.toString(), false);
            }

            event.reply(pings)
                    .addEmbeds(embed.build())
                    .queue();
            return;
        }

        event.reply("Unknown ping type: " + type).setEphemeral(true).queue();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private List<LinkDef> parseLinks(String json) {
        List<LinkDef> out = new ArrayList<>();
        if (json == null) return out;

        Pattern objPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher m = objPattern.matcher(json);
        Pattern nameP = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Pattern urlP = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        while (m.find()) {
            String obj = m.group(1);
            Matcher mn = nameP.matcher(obj);
            Matcher mu = urlP.matcher(obj);
            String name = null, url = null;
            if (mn.find()) name = mn.group(1);
            if (mu.find()) url = mu.group(1);
            if (!isBlank(name) && !isBlank(url)) {
                out.add(new LinkDef(name, url));
            }
        }
        return out;
    }

    private record LinkDef(String name, String url) {}

    private void handleLeaderboard(@NotNull SlashCommandInteractionEvent event) {
        String type = event.getOption("type") != null ? event.getOption("type").getAsString() : "xp";
        String column;
        String title;
        switch (type.toLowerCase()) {
            case "cookies":
                column = "cookies"; title = "🍪 Cookies Leaderboard"; break;
            case "reputation":
                column = "reputation"; title = "⭐ Reputation Leaderboard"; break;
            default:
                column = "xp"; title = "🏆 XP Leaderboard"; break;
        }

        List<String> lines = new ArrayList<>();
        long viewerId = event.getUser().getIdLong();
        int viewerValue = 0;
        int viewerRank = -1;

        String topSql = "SELECT id, username, " + column + " AS val FROM users ORDER BY " + column + " DESC, id ASC LIMIT 10";
        String selfSql = "SELECT " + column + " FROM users WHERE id = ?";
        String rankSql = "SELECT 1 + COUNT(*) FROM users WHERE " + column + " > ?";
        try (Connection c = db.getConnection()) {
            // Top list
            try (PreparedStatement ps = c.prepareStatement(topSql); ResultSet rs = ps.executeQuery()) {
                int pos = 1;
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("username");
                    int val = rs.getInt("val");
                    String label = switch (column) { case "xp" -> val + " XP"; case "cookies" -> val + " 🍪"; default -> val + " rep"; };
                    lines.add("**" + pos + ".** " + name + " — " + label + (id == viewerId ? " ← you" : ""));
                    pos++;
                }
            }

            // Self value
            try (PreparedStatement ps = c.prepareStatement(selfSql)) {
                ps.setLong(1, viewerId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) viewerValue = rs.getInt(1); }
            }
            // Self rank
            try (PreparedStatement ps = c.prepareStatement(rankSql)) {
                ps.setInt(1, viewerValue);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) viewerRank = rs.getInt(1); }
            }
        } catch (SQLException e) {
            event.reply("Database error: " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        if (lines.isEmpty()) lines.add("No users found.");
        EmbedBuilder eb = EmbedUtil.defaultEmbed()
                .setTitle(title)
                .setDescription(String.join("\n", lines))
                .setFooter("Your rank: " + (viewerRank < 0 ? "N/A" : ("#" + viewerRank)) + " • Your " + column + ": " + viewerValue, null);
        event.replyEmbeds(eb.build()).queue();
    }

    private void handleHelp(@NotNull SlashCommandInteractionEvent event) {
        List<String> lines = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT command, description FROM help ORDER BY id ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lines.add("**" + rs.getString(1) + "** — " + rs.getString(2));
            }
        } catch (SQLException e) {
            event.reply("Database error: " + e.getMessage()).setEphemeral(true).queue();
            return;
        }
        if (lines.isEmpty()) lines.add("No help entries found in DB.");
        EmbedBuilder eb = EmbedUtil.defaultEmbed()
                .setTitle("Help — Commands")
                .setDescription(String.join("\n", lines));
        event.replyEmbeds(eb.build()).queue();
    }
}
