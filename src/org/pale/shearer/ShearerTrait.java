package org.pale.shearer;

import java.util.*;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;


//This is your trait that will be applied to a npc using the /trait mytraitname command. 
//Each NPC gets its own instance of this class.
//the Trait class has a reference to the attached NPC class through the protected field 'npc' or getNPC().
//The Trait class also implements Listener so you can add EventHandlers directly to your trait.
@TraitName("shearer") // convenience annotation in recent CitizensAPI versions for specifying trait name
public class ShearerTrait extends net.citizensnpcs.api.trait.Trait {

	public ShearerTrait() {
		super("shearer");
		plugin = JavaPlugin.getPlugin(Plugin.class);
	}

	enum State {
		IDLE,
		STOPPED,
		MOVING_TO_SHEEP,
		MOVING_TO_CONTAINER,
		CONTAINERS_FULL,
		LOOKING_FOR_SHEEP;
	}

	static final int MIN_SHEEP_SCAN = 3;
	static final int MAX_SHEEP_SCAN = 20;
	static final int CONTAINER_DIST = 20;
	static final int MIN_IDLE_TIME = 1000;
	static final int MAX_IDLE_TIME = 2000;
	static final int NAV_TIMEOUT=1000;
	static final int CONTAINERS_FULL_PAUSE = 4000; // containers all full, pause this long then look agian
	static final int WOOL_MAX = 128; // when to move wool to container
	static final double  WANDER_CHANCE = 0.1;
	static final double WANDER_HOME_LIMIT_SQ = 200; // squared limit of wander distance from "home"
	static final double WANDER_HEIGHT_DIST_LIMIT = 3; // limit of height wander distance from "home"

	public static boolean isNotWalkable(Material m){
		return m==Material.IRON_BARS || Tag.FENCES.isTagged(m);
	}

	private int tickint=0;
	public long timeSpawned=0;
	public long timeStateStarted = 0;
	static Random rand = new Random();

	// best to begin stopped, lest we collect a huge amount of wool!
	State state = State.STOPPED;


	private void gotoState(State t){
		Plugin.log("State := "+t.toString());
		state = t;
		timeStateStarted = timeSpawned;
		if(state == State.IDLE){
			idleTime = rand.nextInt(MAX_IDLE_TIME+MIN_IDLE_TIME)+MIN_IDLE_TIME;
		}
	}
	private long stateTime(){
		return timeSpawned - timeStateStarted;
	}



	Plugin plugin = null;
	Location home = null;


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

	
	// Called every tick
	@Override
	public void run() {
		if(tickint++==20){ // to reduce CPU usage - this is about 1Hz.
			update();
			tickint=0;
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

	// start shearing!
	public void start() {
		if(state == State.MOVING_TO_CONTAINER || state == State.MOVING_TO_SHEEP)
			stopNav();
		gotoState(State.IDLE);
	}

	// stop shearing
	public void stop() {
		if(state == State.MOVING_TO_CONTAINER || state == State.MOVING_TO_SHEEP)
			stopNav();
		gotoState(State.STOPPED);
	}

	public void setHome(){
		home = npc.getStoredLocation().clone();
	}

	private void stopNav(){
		getNPC().getNavigator().cancelNavigation();
	}

	int scanDist = MIN_SHEEP_SCAN;
	int idleTime;

	private void update(){
		Plugin.log("Current state "+state.toString()+ " for "+stateTime());
		switch(state){
			case STOPPED:
				// every now and then wander around
				if(rand.nextDouble() < WANDER_CHANCE){
					wander();
				}
				break;
			case IDLE:
				// every now and then wander around
				if(rand.nextDouble() < WANDER_CHANCE){
  					wander();
				}
				if(stateTime()>idleTime) // idleTime gets set by hacky code in gotoState
					gotoState(State.LOOKING_FOR_SHEEP);
				break;
			case LOOKING_FOR_SHEEP:
				findSheepToTarget();
				if(state == State.IDLE) {
					// didn't find one, so gradually increase the scan distance when we can't find one
					scanDist++;
					if(scanDist>MAX_SHEEP_SCAN)scanDist=MAX_SHEEP_SCAN;
				} else
					scanDist = MIN_SHEEP_SCAN; // but always reset it
				break;
			case MOVING_TO_CONTAINER:
				// hopefully, moving to another sheep will help us find a damn container.
				if(stateTime()>NAV_TIMEOUT)
					gotoState(State.IDLE);
				checkNearContainer();
				break;
			case MOVING_TO_SHEEP:
				if(stateTime()>NAV_TIMEOUT)
					gotoState(State.IDLE); // give up, took too long.
				checkNearSheep();
				break;
			case CONTAINERS_FULL:
				if(stateTime()>CONTAINERS_FULL_PAUSE)
					findContainerToTarget();
			default:break;
		}
	}

	private void wander() {
		// just go to a random location. We do this by just finding a point within 10 and going there.
		Location l = npc.getStoredLocation();
		World w = l.getWorld();
		int x = l.getBlockX();
		int z = l.getBlockZ();

		if(home == null)
			setHome();

		for(int i=0;i<100;i++) {

			int bx = x + rand.nextInt(21) - 10;
			int bz = z + rand.nextInt(21) - 10;
			Block b = w.getHighestBlockAt(bx, bz);
			Location newloc = b.getLocation();

			// don't use if it's not ground, or it's too far from home, or the height distance from home is too great
			if(home.distanceSquared(newloc)>WANDER_HOME_LIMIT_SQ ||
					Math.abs(home.getY()-newloc.getY())>WANDER_HEIGHT_DIST_LIMIT ||
					isNotWalkable(b.getType())){
				continue;
			}
			npc.getNavigator().setTarget(b.getLocation().add(0, 1, 0));
			Plugin.log("wander target set");
			break;
		}

	}

	private Sheep sheepTarget = null;
	private void findSheepToTarget(){
		Plugin.log("looking for a sheep at "+scanDist);
		List<Entity> lst = npc.getEntity().getNearbyEntities(scanDist,3,scanDist);
		sheepTarget=null;
		for(Entity e:lst){
			if(e.getType() == EntityType.SHEEP){
				Sheep sheep = (Sheep)e;
				if(!sheep.isSheared()){
					// could look for nearest but this one will do.
					sheepTarget = sheep;
					gotoState(State.MOVING_TO_SHEEP);
					getNPC().getNavigator().setTarget(sheep,false);
				}
			}
		}
		if(sheepTarget == null)
			gotoState(State.IDLE); // should be this anyway.
	}

	static private Material getWoolMat(DyeColor c){
		switch(c) {
			// ugly, ugly.
			case BLACK:
				return Material.BLACK_WOOL;
			case BLUE:
				return Material.BLUE_WOOL;
			case BROWN:
				return Material.BROWN_WOOL;
			case CYAN:
				return Material.CYAN_WOOL;
			case GRAY:
				return Material.GRAY_WOOL;
			case GREEN:
				return Material.GREEN_WOOL;
			case LIGHT_BLUE:
				return Material.LIGHT_BLUE_WOOL;
			case LIGHT_GRAY:
				return Material.LIGHT_GRAY_WOOL;
			case LIME:
				return Material.LIME_WOOL;
			case MAGENTA:
				return Material.MAGENTA_WOOL;
			case ORANGE:
				return Material.ORANGE_WOOL;
			case PINK:
				return Material.PINK_WOOL;
			case PURPLE:
				return Material.PURPLE_WOOL;
			case RED:
				return Material.RED_WOOL;
			case WHITE:
				return Material.WHITE_WOOL;
			default:
			case YELLOW:
				return Material.YELLOW_WOOL;
		}
	}

	private void checkNearSheep(){
		double d = npc.getStoredLocation().distance(sheepTarget.getLocation());
		Plugin.log("dist "+d);
		if(d<2){
			// close enough!
			npc.getNavigator().cancelNavigation();
			Plugin.log("shearing a sheep");
			sheepTarget.setSheared(true);
			ItemStack st = new ItemStack(getWoolMat(sheepTarget.getColor()),rand.nextInt(3)+1);
			if(npc.getEntity() instanceof Player){
				PlayerInventory i = ((Player)(npc.getEntity())).getInventory();
				Plugin.log("player inventory: ");
				int woolcount = 0;
				for(ItemStack ii: i.getContents()){
					if(ii != null) {
						Plugin.log("  Item:" + ii.getType() + " Amount: " + ii.getAmount());
						if(Tag.WOOL.isTagged(ii.getType())){
							woolcount += ii.getAmount();
						}
					}
				}
				if(woolcount > WOOL_MAX || i.addItem(st).size()>0){
					// no space!
					findContainerToTarget();
				} else
					gotoState(State.IDLE);
			} else {
				Plugin.log("not a player, can't get wool!");
			}
		}
	}

	private Block containerTarget;
	private Block findNearestContainer(Material m){
		Location l = npc.getStoredLocation();
		World w = l.getWorld();
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		int closest = -1;
		Block found = null;
		int ylimit =2;
		for(int dx=-CONTAINER_DIST;dx<=CONTAINER_DIST;dx++){
			for(int dy=-ylimit;dy<=ylimit;dy++){
				for(int dz=-CONTAINER_DIST;dz<=CONTAINER_DIST;dz++){
					int bx = x+dx, by = y+dy, bz = z+dz;
					Block b = w.getBlockAt(bx,by,bz);
					if(b.getType()==m){
						// check it will fit
						Container con = (Container)(b.getState());
						Inventory inv = con.getInventory();
						if(inv.firstEmpty()>=0) // yeah, we'll always have one empty slot.
						{
							int dist = dx * dx + dy * dy + dz * dz;
							if (found == null || dist < closest) {
								closest = dist;
								found = b;
							}
						}
					}
				}
			}
		}
		return found;
	}

	private void findContainerToTarget(){
		containerTarget = findNearestContainer(Material.BARREL);
		if(containerTarget != null){
			gotoState(State.MOVING_TO_CONTAINER);
			npc.getNavigator().setTarget(containerTarget.getLocation());
			Plugin.log("found container");
		} else {
			Plugin.log("can't find container");
			gotoState(State.CONTAINERS_FULL); // wait a while
		}
	}

	private void checkNearContainer(){
		double d = npc.getStoredLocation().distance(containerTarget.getLocation());
		Plugin.log("dist "+d);
		if(d<4) {
			npc.getNavigator().cancelNavigation();
			BlockState bs = containerTarget.getState();
			Inventory con = null;
			if(bs instanceof Container){
				con = ((Container) bs).getInventory();
			} else {
				Plugin.log("Container isn't, can't store");
				gotoState(State.STOPPED);
				return;
			}
			if(npc.getEntity() instanceof Player){
				PlayerInventory inv =  ((Player)(npc.getEntity())).getInventory();
				List<ItemStack> rem = new ArrayList<>();
				for(ItemStack st: inv.getContents()){
					if(st!=null) {
						if (Tag.WOOL.isTagged(st.getType())) {
							rem.add(st);
							con.addItem(st);
						}
					}
				}
				for(ItemStack i: rem){
					inv.remove(i);
				}
				Plugin.log("transferred to container");
				gotoState(State.IDLE);
			} else {
				Plugin.log("Not a player, can't put wool!");
				gotoState(State.STOPPED);
			}
		}

	}

}
