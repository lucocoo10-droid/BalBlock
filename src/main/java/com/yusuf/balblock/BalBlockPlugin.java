package com.yusuf.balblock;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BalBlockPlugin extends JavaPlugin {

    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Vault ekonomisi bulunamadi, eklenti devre disi birakiliyor!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("BalBlock aktif!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular icindir.");
            return true;
        }

        String prefix = getConfig().getString("prefix", "&6[BalBlock] &r").replace('&', '§');
        Material targetBlock = Material.matchMaterial(getConfig().getString("block-material", "GOLD_BLOCK"));
        if (targetBlock == null) {
            targetBlock = Material.GOLD_BLOCK;
        }

        if (player.getLocation().subtract(0, 1, 0).getBlock().getType() != targetBlock) {
            player.sendMessage(prefix + "§cBu islemi yapmak icin §e" + targetBlock.name() + " §cblogunun uzerinde durmalisin!");
            return true;
        }

        double balance = economy.getBalance(player);
        ConfigurationSection ranks = getConfig().getConfigurationSection("ranks");
        if (ranks == null) {
            player.sendMessage(prefix + "§cRutbe ayari bulunamadi.");
            return true;
        }

        String targetRank = (args.length > 0) ? args[0].toLowerCase() : null;
        for (String key : ranks.getKeys(false)) {
            if (targetRank != null && !key.equalsIgnoreCase(targetRank)) {
                continue;
            }
            ConfigurationSection sec = ranks.getConfigurationSection(key);
            if (sec == null) continue;

            double req = sec.getDouble("required-balance", 0.0);
            String perm = sec.getString("permission", "");
            String cmd = sec.getString("command", "");

            if (!perm.isEmpty() && player.hasPermission(perm)) {
                if (targetRank != null) {
                    player.sendMessage(prefix + "§aBu rutbeye zaten sahipsin.");
                    return true;
                }
                continue;
            }

            if (balance >= req) {
                if (!cmd.isEmpty()) {
                    String finalCmd = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                }
                player.sendMessage(prefix + "§aTebrikler! §e" + key.toUpperCase() + " §arutbesini acdin.");
                return true;
            } else if (targetRank != null) {
                player.sendMessage(prefix + "§cYeterli bakiye yok! Gereken: §e" + req + " §c| Mevcut: §e" + String.format("%.2f", balance));
                return true;
            }
        }

        player.sendMessage(prefix + "§eSu an acabilecegin yeni bir rutbe bulunmuyor.");
        return true;
    }
}
