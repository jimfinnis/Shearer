package org.pale.shearer;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.citizensnpcs.api.CitizensAPI;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.pale.shearer.Command.*;
import org.bukkit.util.Vector;




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


	private Registry commandRegistry=new Registry();

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

	public Plugin(){
		super();
		if(instance!=null)
			throw new RuntimeException("oi! only one instance!");
	}

	@Override
	public void onEnable() {
		instance = this;
		//check if Citizens is present and enabled.

		if(getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
			getLogger().severe("Citizens 2.0 not found or not enabled");
			getServer().getPluginManager().disablePlugin(this);	
			return;
		}

		//Register.        
		net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(Trait.class));	

		saveDefaultConfig();
		commandRegistry.register(this); // register commands

		getLogger().info("Shearer has been enabled");
	}


	public static void sendCmdMessage(CommandSender s,String msg){
		s.sendMessage(ChatColor.AQUA+"[Shearer] "+ChatColor.YELLOW+msg);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		String cn = command.getName();
		if(cn.equals("shearer")){
			commandRegistry.handleCommand(sender, args);
			return true;
		}
		return false;
	}

	public static Trait getTraitFor(CommandSender sender) {
		NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
		if (npc == null) {
			return null;
		}
		if (npc.hasTrait(Trait.class)) {
			return npc.getTrait(Trait.class);
		}
		return null;
	}


	/**
	 * Commands
	 */
    
    @Cmd(desc="test",argc=0)
          public void test(CallInfo c){
              c.msg("working");
          }
    


	@Cmd(desc="show help for a command or list commands",argc=-1,usage="[<command name>]")
	public void help(CallInfo c){
		if(c.getArgs().length==0){
			commandRegistry.listCommands(c);
		} else {
			commandRegistry.showHelp(c,c.getArgs()[0]);
		}
	}

}
