package org.pale.shearer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.persistence.PersistenceLoader;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;


//This is your trait that will be applied to a npc using the /trait mytraitname command. 
//Each NPC gets its own instance of this class.
//the Trait class has a reference to the attached NPC class through the protected field 'npc' or getNPC().
//The Trait class also implements Listener so you can add EventHandlers directly to your trait.
@TraitName("shearer") // convenience annotation in recent CitizensAPI versions for specifying trait name
public class Trait extends net.citizensnpcs.api.trait.Trait {

	public Trait() {
		super("shearer");
		plugin = JavaPlugin.getPlugin(Plugin.class);
	}

	static Random rand = new Random();

	Plugin plugin = null;


	// 
	// Here you should load up any values you have previously saved (optional). 
	// This does NOT get called when applying the trait for the first time, only loading onto an existing npc at server start.
	// This is called AFTER onAttach so you can load defaults in onAttach and they will be overridden here.
	// This is called BEFORE onSpawn, npc.getBukkitEntity() will return null.
	public void load(DataKey key) {
//		SomeSetting = key.getBoolean("SomeSetting", false);
		
	}

	// Save settings for this NPC (optional). These values will be persisted to the Citizens saves file
	public void save(DataKey key) {
//		key.setBoolean("SomeSetting",SomeSetting);
	}

	private int tickint=0;
	public long timeSpawned=0;
	
	// Called every tick
	@Override
	public void run() {
		if(tickint++==20){ // to reduce CPU usage - this is about 1Hz.
		}
		timeSpawned++;
	}

	//Run code when your trait is attached to a NPC. 
	//This is called BEFORE onSpawn, so npc.getBukkitEntity() will return null
	//This would be a good place to load configurable defaults for new NPCs.
	@Override
	public void onAttach() {
		plugin.getServer().getLogger().info(npc.getName() + " has been assigned Shearer!");
	}

	// Run code when the NPC is despawned. This is called before the entity actually despawns so npc.getBukkitEntity() is still valid.
	@Override
	public void onDespawn() {
		Plugin.log(" Despawn run on "+npc.getFullName());
	}

	//Run code when the NPC is spawned. Note that npc.getBukkitEntity() will be null until this method is called.
	//This is called AFTER onAttach and AFTER Load when the server is started.
	@Override
	public void onSpawn() {
		Plugin.log(" Spawn run on "+npc.getFullName());
	}

	//run code when the NPC is removed. Use this to tear down any repeating tasks.
	@Override
	public void onRemove() {
	}

}
