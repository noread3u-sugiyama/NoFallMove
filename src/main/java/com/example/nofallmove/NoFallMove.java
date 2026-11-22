package com.example.nofallmove;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NoFallMove extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Set<String> targetChunks = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadChunks();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("nfm") != null) {
            getCommand("nfm").setExecutor(this);
            getCommand("nfm").setTabCompleter(this);
        }
    }

    private void loadChunks() {
        targetChunks.clear();
        targetChunks.addAll(getConfig().getStringList("chunks"));
    }

    private void saveChunks() {
        getConfig().set("chunks", new ArrayList<>(targetChunks));
        saveConfig();
    }

    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        if (!(event.getEntity() instanceof Monster mob)) {
            return;
        }

        Location from = event.getFrom();
        String key = from.getWorld().getName() + ":" + (from.getBlockX() >> 4) + ":" + (from.getBlockZ() >> 4);
        if (!targetChunks.contains(key)) {
            return;
        }

        Location to = event.getTo();
        Vector vel = mob.getVelocity();

        if (mob instanceof Spider && !mob.isOnGround()) {
            if (to.getY() > from.getY() || vel.getY() > 0.05) {
                mob.getPathfinder().stopPathfinding();
                mob.setVelocity(new Vector(0, -0.15, 0));
                to.setX(from.getX());
                to.setY(from.getY());
                to.setZ(from.getZ());
                event.setTo(to);
                return;
            }
        }

        if (!mob.isOnGround() && (mob.getFallDistance() > 0.25f || vel.getY() < -0.12)) {
            mob.getPathfinder().stopPathfinding();

            if (vel.getX() != 0 || vel.getZ() != 0) {
                mob.setVelocity(new Vector(0, vel.getY(), 0));
            }

            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                to.setX(from.getX());
                to.setZ(from.getZ());
                event.setTo(to);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> argList = Arrays.asList(args);
        if (argList.isEmpty()) {
            sender.sendMessage("§e使用方法: /nfm <add|remove|list|reload>");
            return true;
        }

        String sub = argList.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cプレイヤーのみ実行できます。");
                    return true;
                }
                String key = getChunkKey(player.getLocation().getChunk());
                if (targetChunks.add(key)) {
                    saveChunks();
                    sender.sendMessage("§aチャンク [" + key + "] を無効エリアに追加しました。");
                } else {
                    sender.sendMessage("§eこのチャンクは既に登録されています。");
                }
            }
            case "remove" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cプレイヤーのみ実行できます。");
                    return true;
                }
                String key = getChunkKey(player.getLocation().getChunk());
                if (targetChunks.remove(key)) {
                    saveChunks();
                    sender.sendMessage("§aチャンク [" + key + "] を無効エリアから削除しました。");
                } else {
                    sender.sendMessage("§eこのチャンクは登録されていません。");
                }
            }
            case "list" -> {
                sender.sendMessage("§6登録済みチャンク一覧 (" + targetChunks.size() + "件):");
                for (String key : targetChunks) {
                    sender.sendMessage(" §7- " + key);
                }
            }
            case "reload" -> {
                reloadConfig();
                loadChunks();
                sender.sendMessage("§a設定を再読み込みしました。");
            }
            default -> sender.sendMessage("§e使用方法: /nfm <add|remove|list|reload>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> argList = Arrays.asList(args);
        if (argList.size() == 1) {
            String input = argList.get(0).toLowerCase(Locale.ROOT);
            return List.of("add", "remove", "list", "reload").stream()
                    .filter(s -> s.startsWith(input))
                    .toList();
        }
        return List.of();
    }
}
