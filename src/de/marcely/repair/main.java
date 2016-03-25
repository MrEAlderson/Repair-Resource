package de.marcely.repair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class main extends JavaPlugin implements CommandExecutor {
	// basic variables
	public static Plugin plugin = null;
	public static String version = "1.3.6";
	public ConfigManager cm = new ConfigManager(this.getName(), "config.yml");
	
	// variables for the '/fix user' command
	private HashMap<Player, Player> playersLookingInInv = new HashMap<Player, Player>();
	private ArrayList<Player> cache = new ArrayList<Player>();
	
	// variables for the cooldown
	private ArrayList<String> cooldownplayers = new ArrayList<String>();
	
	// config variables
	public boolean CONFIG_LORES = true;
	public boolean CONFIG_ANVILLORES = true;
	public boolean CONFIG_NOTOOLDAMAGE = false;
	public boolean CONFIG_COOLDOWN = false;
	public double CONFIG_COOLDOWNTIME = 60;
	public String CONFIG_VERSION = "0";
	public ArrayList<Material> CONFIG_BLACKLISTEDTOOLS = new ArrayList<Material>();
	
	@Override
	public void onEnable(){ 
		plugin = this;
		getCommand("repair").setExecutor(this);
		getCommand("fix").setExecutor(this);
		getServer().getPluginManager().registerEvents(listener, this);
		
		// create/load config
		if(cm.exists() == false){
			saveConfig();
		}
		loadConfig();
	}
	
	@Override
	public void onDisable(){ }
	
	public void updateInventoryScheduler(Player lookingPlayer, Player fromPlayer){
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			@Override
			public void run() {
				Inventory oInv = Bukkit.createInventory(lookingPlayer, 6*9, ChatColor.GOLD + "Inventory by " + ChatColor.YELLOW + fromPlayer.getName());
				lookingPlayer.openInventory(updateInventory(oInv, fromPlayer));
			}
		}, 1);
	}
	
	public void removePlayerFromCooldownScheduler(Player player){
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			@Override
			public void run() {
				if(cooldownplayers.contains(player.getName())){
					cooldownplayers.remove(player.getName());
				}else{
					Bukkit.getConsoleSender().sendMessage("Something strange happend! (1)");
				}
			}
		}, (int)(getCooldownTime(player) * 20));
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(sender instanceof Player){
				Player player = (Player) sender;
				if(args.length >= 1 && args[0].equalsIgnoreCase("all") || args.length >= 1 && args[0].equalsIgnoreCase("everything")){ // REPAIR
					if(sender.hasPermission("repair.cmd")){
						if(playerIsInCooldown(player) == false){
							HashMap<ItemStack, Integer> repairAbleItems = new HashMap<ItemStack, Integer>();
			
							ItemStack helmet = player.getInventory().getHelmet();
							ItemStack chestplate = player.getInventory().getChestplate();
							ItemStack leggings = player.getInventory().getLeggings();
							ItemStack boots = player.getInventory().getBoots();
							if(helmet != null && isRepairAble(helmet.getType()) && loreFine(helmet)) repairAbleItems.put(helmet, -1);
							if(chestplate != null && isRepairAble(chestplate.getType()) && loreFine(chestplate)) repairAbleItems.put(chestplate, -2);
							if(leggings != null && isRepairAble(leggings.getType()) && loreFine(leggings)) repairAbleItems.put(leggings, -3);
							if(boots != null && isRepairAble(boots.getType()) && loreFine(boots)) repairAbleItems.put(boots, -4);
							
							
							int i=0;
							for(ItemStack is:player.getInventory().getContents()){
								if(is != null && isRepairAble(is.getType()) && loreFine(is))
									repairAbleItems.put(is, i);
								i++;
							}
							
							if(repairAbleItems.size() >= 1){
								for(Entry<ItemStack, Integer> entry:repairAbleItems.entrySet()){
									ItemStack is = entry.getKey();
									Integer position = entry.getValue();
									is.setDurability((short) 0);
									if(position == -1)
										player.getInventory().setHelmet(is);
									else if(position == -2)
										player.getInventory().setChestplate(is);
									else if(position == -3)
										player.getInventory().setLeggings(is);
									else if(position == -4)
										player.getInventory().setBoots(is);
									else
										player.getInventory().setItem(position, is);
								}
								player.sendMessage(ChatColor.GREEN + "" + repairAbleItems.size() + " item(s) has been repaired!");
								if(CONFIG_COOLDOWN == true)
									addPlayerToCooldown(player);
							}else{
								player.sendMessage(ChatColor.RED + "None item of your inventory is a repairable item!");
							}
						}else{
							sendPlayerWaitMessage(player);
						}
					}else{
						player.sendMessage(ChatColor.RED + "You have got no permissions for this command!");
					}
				}else if(args.length >= 1 && args[0].equalsIgnoreCase("user")){ // USER
					if(sender.hasPermission("repair.user")){
						if(args.length >= 2){
							Player op = getOnlinePlayer(args[1]);
							if(op != null){
								if(!op.equals(player)){
									Inventory inv = Bukkit.createInventory(player, 6*9, ChatColor.GOLD + "Inventory by " + ChatColor.YELLOW + op.getName());
									player.openInventory(updateInventory(inv, op));
									playersLookingInInv.put(player, op);
								}else{
									player.sendMessage(ChatColor.RED + "You can't look in your own inventory!");
								}
							}else{
								player.sendMessage(ChatColor.RED + "'" + args[1] + "' is not online!");
							}
						}else{
							player.sendMessage(ChatColor.GOLD + "Use this command like this: " + ChatColor.YELLOW + "/" + label + " user <username>");
						}
					}else{
						player.sendMessage(ChatColor.RED + "You have got no permissions for this command!");
					}
				}else if(args.length >= 1 && args[0].equalsIgnoreCase("reload")){ // RELOAD
					if(player.hasPermission("repair.reload")){
						loadConfig();
						player.sendMessage(ChatColor.DARK_GREEN + "The config " + ChatColor.GREEN + "has been succesfully reloaded!");
					}else{
						player.sendMessage(ChatColor.RED + "You have got no permissions for this command!");
					}
				}else{
					if(player.hasPermission("repair.cmd")){ // ELSE
						if(playerIsInCooldown(player) == false){
							ItemStack repairingItem = player.getItemInHand();
							if(isRepairAble(repairingItem.getType()) && loreFine(repairingItem)){
								repairingItem.setDurability((short) 0);
								player.setItemInHand(repairingItem);
								player.sendMessage(ChatColor.GREEN + "Item '" + repairingItem.getType().name().toLowerCase().replace("_", "") + "' has been successfully repaired!");
								if(CONFIG_COOLDOWN == true)
									addPlayerToCooldown(player);
							}else{
								player.sendMessage(ChatColor.RED + "The item you are holding, is not repairable!");
							}
						}else{
							sendPlayerWaitMessage(player);
						}
					}else{
						player.sendMessage(ChatColor.RED + "You have got no permissions for this command!");
					}
				}
		}else{
			sender.sendMessage(ChatColor.RED + "You are not a Player!");
		}
		return true;
	}
	
	private Listener listener = new Listener(){
		@EventHandler
		public void onInventoryClick(InventoryClickEvent event){
			Player player = (Player) event.getWhoClicked();
			
			/*ItemStack iss = new ItemStack(Material.WOOD_SWORD);
			ItemMeta imm = iss.getItemMeta();
			List<String> l = new ArrayList<String>();
			l.add("test");
			imm.setLore(l);
			iss.setItemMeta(imm);
			player.getInventory().addItem(iss);*/
			
			Inventory inv = event.getClickedInventory();
			ItemStack is = event.getCurrentItem();
			if(player == null || inv == null || inv.getType() == null)
				return;
			InventoryType type = inv.getType();
			if(type == InventoryType.CHEST && inv.getTitle() != null && is != null && is.getType() != null && is.getType() != Material.AIR){
				String title = inv.getTitle();
				Material clickedItem = is.getType();
				if(!title.startsWith(ChatColor.GOLD + "Inventory by " + ChatColor.YELLOW))
					return;
				String strInvOwner = title.replace(ChatColor.GOLD + "Inventory by " + ChatColor.YELLOW, "");
				Player invOwner = getOnlinePlayer(strInvOwner);
				event.setCancelled(true);
				if(invOwner == null){
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "'" + strInvOwner + "' left this server!");
				}
				if(isRepairAble(clickedItem)){
					int slot = event.getSlot();
					if(slot >= 45) slot -= 45;
					else if(slot >= 18 && slot < 45) slot -= 9;
					is.setDurability((short) 0);
					player.sendMessage(slot + "");
					invOwner.getInventory().setItem(slot, is);
					playSound(player, Sound.LEVEL_UP);
				}else{
					playSound(player, Sound.NOTE_BASS_DRUM);
				}
			}else if(type == InventoryType.PLAYER && playersLookingInInv.containsValue(player)){
				ArrayList<Player> lookingPlayers = getWhoLookingPlayers(player);
				for(Player lookingPlayer:lookingPlayers){
					cache.add(lookingPlayer);
					updateInventoryScheduler(lookingPlayer, player);
				}
			}else if(type != null && event.getInventory() != null && type == InventoryType.ANVIL && CONFIG_ANVILLORES == false && event.getSlot() == 2 && event.getInventory().getItem(event.getSlot()) != null && event.getInventory().getItem(event.getSlot()).getItemMeta().getLore() != null && event.getInventory().getItem(event.getSlot()).getItemMeta().getLore().size() >= 1){
				event.setCancelled(true);
				player.closeInventory();
				player.sendMessage(ChatColor.RED + "This isn't currently working!");
			}
		}
		
		@EventHandler
		public void onInventoryClose(InventoryCloseEvent event){
			Player player = (Player) event.getPlayer();
			if(!cache.contains(player)){
				if(playersLookingInInv.containsKey(player)){
					playersLookingInInv.remove(player);
				}
			}else{
				cache.remove(player);
			}
		}
		
		@EventHandler
		public void onPlayerItemDamage(PlayerItemDamageEvent event){
			int damage = event.getDamage();
			Player player = event.getPlayer();
			if(CONFIG_NOTOOLDAMAGE == true && damage > 0 && player.hasPermission("repair.notooldamage") && !CONFIG_BLACKLISTEDTOOLS.contains(event.getItem().getType())){
				event.setCancelled(true);
			}
		}
	};
	
	public ArrayList<Player> getWhoLookingPlayers(Player player){
		ArrayList<Player> players = new ArrayList<Player>();
		for(Entry<Player, Player> entry:playersLookingInInv.entrySet()){
			if(entry.getValue().equals(player))
				players.add(entry.getKey());
		}
		
		return players;
	}
	
	public ItemStack getItemStack(Material material, Short id, String title){
		ItemStack is = new ItemStack(material, 1, id);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(title);
		is.setItemMeta(im);
		return is;
	}
	
	public ItemStack getItemStack(Material material, String title){
		return getItemStack(material, (short) 0, title);
	}
	
    public static void playSound(Player player, Sound sound){
    	player.playSound(player.getLocation(), sound, 100F, 1F);
    }
    
    public Inventory updateInventory(Inventory inv, Player fromPlayer){
    	PlayerInventory opInv = fromPlayer.getInventory();
    	
    	// black line
		for(int i=0; i<9; i++){
			inv.setItem(i + 9, getItemStack(Material.STAINED_GLASS_PANE, (short) 15, " "));
		}
		
		// inv items
		int i=0;
		for(ItemStack is:opInv.getContents()){
			if(i >= 9){
				inv.setItem(i + 9, is);
			}else{
				inv.setItem(i + 5*9, is);
			}
			i++;
		}
		
		// armor
		ItemStack helmet = opInv.getHelmet();
		ItemStack chestplate = opInv.getChestplate();
		ItemStack leggings = opInv.getLeggings();
		ItemStack boots = opInv.getBoots();
		if(helmet == null || helmet.getType() == null)
			helmet = getItemStack(Material.BARRIER, ChatColor.RED + "No helmet");
		if(chestplate == null || chestplate.getType() == null)
			chestplate = getItemStack(Material.BARRIER, ChatColor.RED + "No chestplate");
		if(leggings == null || leggings.getType() == null)
			leggings = getItemStack(Material.BARRIER, ChatColor.RED + "No leggings");
		if(boots == null || boots.getType() == null)
			boots = getItemStack(Material.BARRIER, ChatColor.RED + "No boots");
		
		inv.setItem(0, helmet);
		inv.setItem(1, chestplate);
		inv.setItem(2, leggings);
		inv.setItem(3, boots);
		
		return inv;
    }
    
	public boolean isRepairAble(Material material){
		if(material == null) return false;
		if(CONFIG_BLACKLISTEDTOOLS.contains(material)) return false;
		
		if(material == Material.WOOD_AXE ||
		   material == Material.WOOD_HOE ||
		   material == Material.WOOD_SWORD ||
		   material == Material.WOOD_SPADE ||
		   material == Material.WOOD_PICKAXE ||
		   material == Material.STONE_AXE ||
		   material == Material.STONE_HOE ||
		   material == Material.STONE_SWORD ||
		   material == Material.STONE_SPADE ||
		   material == Material.STONE_PICKAXE ||
		   material == Material.GOLD_AXE ||
		   material == Material.GOLD_HOE ||
		   material == Material.GOLD_SWORD ||
		   material == Material.GOLD_SPADE ||
		   material == Material.GOLD_PICKAXE ||
		   material == Material.IRON_AXE ||
		   material == Material.IRON_HOE ||
		   material == Material.IRON_SWORD ||
		   material == Material.IRON_SPADE ||
		   material == Material.IRON_PICKAXE ||
		   material == Material.DIAMOND_AXE ||
		   material == Material.DIAMOND_HOE ||
		   material == Material.DIAMOND_SWORD ||
		   material == Material.DIAMOND_SPADE ||
		   material == Material.DIAMOND_PICKAXE ||
		   
		   material == Material.DIAMOND_BOOTS ||
		   material == Material.DIAMOND_CHESTPLATE ||
		   material == Material.DIAMOND_HELMET ||
		   material == Material.DIAMOND_LEGGINGS ||
		   material == Material.IRON_BOOTS ||
		   material == Material.IRON_CHESTPLATE ||
		   material == Material.IRON_HELMET ||
		   material == Material.IRON_LEGGINGS ||
		   material == Material.GOLD_BOOTS ||
		   material == Material.GOLD_CHESTPLATE ||
		   material == Material.GOLD_HELMET ||
		   material == Material.GOLD_LEGGINGS ||
		   material == Material.DIAMOND_BOOTS ||
		   material == Material.DIAMOND_CHESTPLATE ||
		   material == Material.DIAMOND_HELMET ||
		   material == Material.DIAMOND_LEGGINGS ||
		   material == Material.CHAINMAIL_BOOTS ||
		   material == Material.CHAINMAIL_CHESTPLATE ||
	       material == Material.CHAINMAIL_HELMET ||
		   material == Material.CHAINMAIL_LEGGINGS ||
				   
		   material == Material.FISHING_ROD ||
		   material == Material.SHEARS ||
		   material == Material.FLINT_AND_STEEL ||
		   material == Material.BOW)
			return true;
		
		return false;
	}
	
	public boolean loreFine(ItemStack is){
		List<String> lores = is.getItemMeta().getLore();
		if(lores == null)
			return true;
		if(CONFIG_LORES == false && lores.size() >= 1)
			return false;
		
		return true;
	}
	
	public Player getOnlinePlayer(String name){
		for(Player player:Bukkit.getOnlinePlayers()){
			if(player.getName().equalsIgnoreCase(name))
				return player;
		}
		
		return null;
	}
	
	public void addPlayerToCooldown(Player player){
		cooldownplayers.add(player.getName());
		removePlayerFromCooldownScheduler(player);
	}
	
	public boolean playerIsInCooldown(Player player){
		if(CONFIG_COOLDOWN == true && cooldownplayers.contains(player.getName()) && !player.hasPermission("repair.skipcooldown"))
			return true;
		
		return false;
	}
	
	public double getCooldownTime(Player player){
		HashMap<String, String> list = cm.getKeysWhichStartWith("add_cooldowngroup.");
		for(Entry<String, String> entry:list.entrySet()){
			String groupname = entry.getKey();
			double time = Double.valueOf(entry.getValue());
			if(player.hasPermission("repair.cooldown." + groupname.replace("add_cooldowngroup.", "")))
				return time;
		}
		return CONFIG_COOLDOWNTIME;
	}
	
	public void sendPlayerWaitMessage(Player player){
		double s = getCooldownTime(player);
		int minutes = (int)(s / 60);
		int secounds = (int) (s - (minutes * 60));
		if(minutes >= 1 && secounds == 0){
			if(minutes == 1)
				player.sendMessage(ChatColor.RED + "Wait at least " + minutes + " minute to use this command again!");
			else
				player.sendMessage(ChatColor.RED + "Wait at least " + minutes + " minutes to use this command again!");
		}else if(minutes >= 1){
			if(minutes == 1 && secounds > 0)
				player.sendMessage(ChatColor.RED + "Wait at least " + minutes + " minute and " + secounds + " secounds to use this command again!");
			else if(minutes > 1 && secounds > 0)
				player.sendMessage(ChatColor.RED + "Wait at least " + minutes + " minutes and " + secounds + " secounds to use this command again!");
			else if(minutes == 1 && secounds == 1)
				player.sendMessage(ChatColor.RED + "Wait at least " + minutes + " minute and " + secounds + " secound to use this command again!");
			else if(minutes > 1 && secounds == 1)
				player.sendMessage(ChatColor.RED + "Wait at least " + minutes + " minutes and " + secounds + " secound to use this command again!");
		}else{
			if(secounds == 1)
				player.sendMessage(ChatColor.RED + "Wait at least " + secounds + " secound to use this command again!");
			else
				player.sendMessage(ChatColor.RED + "Wait at least " + secounds + " secounds to use this command again!");
		}
	}
	
	public void loadConfig(){
		cm.load();
		String config_version = cm.getConfigString("config-version");
		boolnull config_lores = cm.getConfigBoolean("lores");
		boolnull config_anvillores = cm.getConfigBoolean("anvil.lores");
		boolnull config_notooldamage = cm.getConfigBoolean("no-tool-damage");
		boolnull config_cooldown = cm.getConfigBoolean("cooldown");
		double config_cooldowntime = cm.getConfigDouble("cooldown_time");
		String config_toolblacklist = cm.getConfigString("tool_blacklist");
		
		if(config_version != null)
			CONFIG_VERSION = config_version;
		
		if(config_lores != boolnull.NULL)
			CONFIG_LORES = config_lores.toBoolean();
		
		if(config_anvillores != boolnull.NULL)
			CONFIG_ANVILLORES = config_anvillores.toBoolean();
		
		if(config_notooldamage != boolnull.NULL)
			CONFIG_NOTOOLDAMAGE = config_notooldamage.toBoolean();
		
		if(config_cooldown != boolnull.NULL)
			CONFIG_COOLDOWN = config_cooldown.toBoolean();
		
		if(config_cooldowntime != Double.MAX_VALUE)
			CONFIG_COOLDOWNTIME = config_cooldowntime;
		
		if(config_toolblacklist != null){
			config_toolblacklist = config_toolblacklist.replace(", ", "");
			String[] strs = config_toolblacklist.split(",");
			if(!strs[0].isEmpty()){
				for(String str:strs){
					Material material = Material.getMaterial(str.toUpperCase());
					if(material == null && isDouble(str))
						material = Material.getMaterial(str);
					if(material != null)
						CONFIG_BLACKLISTEDTOOLS.add(material);
				}
			}
		}
		
		if(config_version == null || config_version != null && !config_version.equals(version))
			saveConfig();
	}
	
	public boolean isDouble(String str){
		try{
			Double.valueOf(str);
		}catch(Exception e){
			return false;
		}
		
		return true;
	}
	
	public void saveConfig(){
		cm.clear();
		cm.addComment("Don't change this");
		cm.addConfig("config-version", version);
		
		cm.addEmptyLine();
		
		cm.addComment("If its disabled (lores: false), tools with lores won't be repaired");
		cm.addConfig("lores", CONFIG_LORES);
		
		cm.addEmptyLine();
		
		cm.addComment("If its disabled (lores.anvil: false), tools in anvils which got lores won't be repaired");
		cm.addConfig("anvil.lores", CONFIG_ANVILLORES);
		
		cm.addEmptyLine();
		
		cm.addComment("If its enabled, the tools from the people with the permission 'repair.notooldamage' won't get any damage");
		cm.addConfig("no-tool-damage", CONFIG_NOTOOLDAMAGE);
		
		cm.addEmptyLine();
		
		cm.addComment("Add tools which aren't allowed to be repaired");
		cm.addComment("Example: tool_blacklist: diamond_sword,woord_spade");
		cm.addConfig("tool_blacklist", "");
		
		cm.addEmptyLine();
		
		cm.addComment("If its enabled, people have to wait until they can use these commands again (people with the permission 'repair.skipcooldown' skip the cooldown)");
		cm.addConfig("cooldown", CONFIG_COOLDOWN);
		
		cm.addEmptyLine();
		
		cm.addComment("The time from the cooldown (1 = 1 secound)");
		cm.addConfig("cooldown_time", CONFIG_COOLDOWNTIME);
		
		cm.addEmptyLine();
		
		cm.addComment("A example, how to add cooldown groups");
		cm.addComment("How it works: add_cooldowngroup.<groupname>: <time>");
		cm.addComment("The permission to add players to this group: repair.cooldown.<groupname>");
		cm.addConfig("add_cooldowngroup.example", 67);
		cm.save();
	}
}
