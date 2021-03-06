package org.pale.shearer;

import net.citizensnpcs.api.CitizensAPI;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.pale.shearer.Command.*;


public class Plugin extends JavaPlugin {
    public static void log(String msg) {
        getInstance().getLogger().info(msg);
    }

    public static void warn(String msg) {
        getInstance().getLogger().warning(msg);
    }

    /**
     * Make the plugin a weird singleton.
     */
    static Plugin instance = null;


    private Registry commandRegistry = new Registry();

    /**
     * Use this to get plugin instances - don't play silly buggers creating new
     * ones all over the place!
     */
    public static Plugin getInstance() {
        if (instance == null)
            throw new RuntimeException(
                    "Attempt to get plugin when it's not enabled");
        return instance;
    }

    @Override
    public void onDisable() {
        instance = null;
        getLogger().info("Shearer has been disabled");
    }

    public Plugin() {
        super();
        if (instance != null)
            throw new RuntimeException("oi! only one instance!");
    }

    @Override
    public void onEnable() {
        instance = this;
        //check if Citizens is present and enabled.

        if (getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
            getLogger().severe("Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //Register.
        net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(ShearerTrait.class));

        saveDefaultConfig();
        commandRegistry.register(this); // register commands

        getLogger().info("Shearer has been enabled");
    }


    public static void sendCmdMessage(CommandSender s, String msg) {
        s.sendMessage(ChatColor.AQUA + "[Shearer] " + ChatColor.YELLOW + msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        String cn = command.getName();
        if (cn.equals("shearer")) {
            commandRegistry.handleCommand(sender, args);
            return true;
        }
        return false;
    }

    public static ShearerTrait getTraitFor(NPC npc) {
        if (npc == null) {
            return null;
        }
        if (npc.hasTrait(ShearerTrait.class)) {
            return npc.getTrait(ShearerTrait.class);
        }
        return null;

    }

    public static ShearerTrait getTraitFor(CommandSender sender) {
        return getTraitFor(CitizensAPI.getDefaultNPCSelector().getSelected(sender));
    }

    @EventHandler
    public void onNavigationComplete(net.citizensnpcs.api.ai.event.NavigationCompleteEvent e){
        Bukkit.broadcastMessage("Nav complete");
    }


    /**
     * Commands
     */

    @Cmd(desc = "start shearing", argc = 0,cz=true)
    public void start(CallInfo c) {
        c.msg("starting work..");
        c.getCitizen().start();
    }
    @Cmd(desc = "stop shearing", argc = 0,cz=true)
    public void stop(CallInfo c) {
        c.msg("stopping work..");
        c.getCitizen().stop();
    }
    @Cmd(desc = "set home location for wandering", argc = 0,cz=true)
    public void home(CallInfo c) {
        c.msg("home set..");
        c.getCitizen().setHome();
    }


    @Cmd(desc = "show help for a command or list commands", argc = -1, usage = "[<command name>]")
    public void help(CallInfo c) {
        if (c.getArgs().length == 0) {
            commandRegistry.listCommands(c);
        } else {
            commandRegistry.showHelp(c, c.getArgs()[0]);
        }
    }

}
