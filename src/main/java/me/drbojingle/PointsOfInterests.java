package me.drbojingle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.drbojingle.Metrics;
import me.drbojingle.POIArea;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

@SuppressWarnings("deprecation")
public class PointsOfInterests extends JavaPlugin
{
	public String adminPermission = "PointsOfInterests.admin";
	public String usePermission = "PointsOfInterests.use";
	public File areasFile;
	public YamlConfiguration yamlAreas;
	public HashMap<String, Location> firstPos  = new HashMap<String, Location>();
	public HashMap<String, Location> secondPos = new HashMap<String, Location>();
	public Map<String, POIArea>      areas     = new HashMap<String, POIArea>();

	@Override
	public void onEnable()
	{
		metrics();
		areasFile = new File("plugins/PointsOfInterests/areas.yml");
		yamlAreas = YamlConfiguration.loadConfiguration(areasFile);
		loadAreas();
		startLoop();
		info("Enabled.");
	}

	@Override
	public void onDisable()
	{
		saveAreas();
		Bukkit.getScheduler().cancelTasks(this);
		info("Disabled.");
	}
	
	public void checkPlayer(Player player)
	{
		if(player.hasPermission(usePermission))
		{
			Location loc = player.getLocation();
			POIArea area = null;
			for(POIArea curArea : areas.values())
				if(curArea.hasInside(loc))
				{
					area = curArea;
					break;
				}
			if(area != null)
			{
				String playerName = player.getName();
				String areaName = area.getName();
				if(!yamlAreas.contains(areaName + ".vis"))
					visit(area, player);
				else
				{
					List<String> visitors = yamlAreas.getStringList(areaName + ".vis");
					if(!visitors.contains(playerName))
						visit(area, player);
				}
			}
		}
	}
	
	public void loopTick()
	{
		for(Player player : Bukkit.getOnlinePlayers())
			checkPlayer(player);
	}
	
	public void startLoop()
	{
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

			@Override
			public void run()
			{
				loopTick();
			}
			
		}, 10L, 10L);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("poi"))
			if(sender.hasPermission(adminPermission))
			{
				if(args.length <= 0)
					listCommands(sender);
				else if(args[0].equalsIgnoreCase("pos1"))
				{
					if(!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "Only for players! Sorry, console.");
						return true;
					}
					Player player = (Player) sender;
					String playerName = player.getName();
					Location loc = player.getLocation();
					firstPos.put(playerName, loc);
					player.sendMessage(ChatColor.GREEN + "First position set!");
				}
				else if(args[0].equalsIgnoreCase("pos2"))
				{
					if(!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "Only for players! Sorry, console.");
						return true;
					}
					Player player = (Player) sender;
					String playerName = player.getName();
					Location loc = player.getLocation();
					secondPos.put(playerName, loc);
					player.sendMessage(ChatColor.GREEN + "Second position set!");
				}
				else if(args[0].equalsIgnoreCase("create"))
				{
					if(!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "Only for players! Sorry, console.");
						return true;
					}
					Player player = (Player) sender;
					if(args.length <= 1)
						player.sendMessage(getCommandInfo("create [Name]", "Creates a point of interests"));
					else
					{
						for(POIArea area : areas.values())
							if(area.getName().equals(args[1]))
							{
								player.sendMessage(ChatColor.RED + "Area with name '" + args[1] + "' already exists!");
								return true;
							}
						String playerName = player.getName();
						boolean selected = firstPos.containsKey(playerName) && secondPos.containsKey(playerName);
						if(!selected)
							player.sendMessage(ChatColor.RED + "First select the area!");
						else
						{
							Location pos1 = firstPos.get(playerName);
							Location pos2 = secondPos.get(playerName);
							if(pos1.getWorld() != pos2.getWorld())
							{
								player.sendMessage(ChatColor.RED + "You have to select an area in one world.");
								return true;
							}
							String name = args[1];
							POIArea area = createArea(name, pos1, pos2);
							area.addMessage(ChatColor.GOLD + "You have reached the '" + name + "'!");
							player.sendMessage(ChatColor.GREEN + "Area successfully created with name '" + name + "'.");
						}
					}
				}
				else if(args[0].equalsIgnoreCase("remove"))
				{
					if(args.length <= 1)
						sender.sendMessage(getCommandInfo("remove [Name]", "Removes a point of interests"));
					else
					{
						String name = removeArea(args[1]);
						if(name != null)
							sender.sendMessage(ChatColor.GREEN + "Successfully removed area " + name);
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("setExperience") || args[0].equalsIgnoreCase("setExp"))
				{ // EXPERIENCE
					if(args.length <= 2)
						sender.sendMessage(getCommandInfo("setExperience [Name] [Experience]", "Assigns experience points to a point of interests"));
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							int exp;
							try
							{
								exp = Integer.parseInt(args[2]);
							}
							catch(NumberFormatException e)
							{
								sender.sendMessage(ChatColor.RED + "Incorrect Item ID!");
								return true;
							}
							area.setExperience(exp);
							saveArea(area);
							sender.sendMessage(ChatColor.GREEN + "Experience succesfully set! (" + exp + ")");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("addReward"))
				{ // REWARDS
					if(args.length <= 2)
						sender.sendMessage(getCommandInfo("addReward [Name] [ItemID] (Data) (Amount)", "Assigns a reward to a point of interests"));
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							int itemId;
							int data = 0;
							int amount = 1;
							try
							{
								itemId = Integer.parseInt(args[2]);
							}
							catch(NumberFormatException e)
							{
								Material mat = Material.getMaterial(args[2]);
								if(mat != null)
									itemId = mat.getId();
								else
								{
									sender.sendMessage(ChatColor.RED + "Invalid Item ID!");
									return true;
								}
							}
							if(args.length >= 4)
							{
								try
								{
									data = Integer.parseInt(args[3]);
								}
								catch(NumberFormatException e)
								{
									sender.sendMessage(ChatColor.RED + "The data has to be a whole number!");
									return true;
								}
								if(args.length >= 5)
									try
									{
										amount = Integer.parseInt(args[4]);
									}
									catch(NumberFormatException e)
									{
										sender.sendMessage(ChatColor.RED + "The amount has to be a whole number!");
										return true;
									}
							}
							ItemStack reward = new ItemStack(itemId, amount, (short) data);
							area.addReward(reward);
							saveArea(area);
							sender.sendMessage(ChatColor.GREEN + "Successfully added a reward! (" + Material.getMaterial(itemId) + ")");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("removeReward"))
				{
					if(args.length <= 2)
						sender.sendMessage(getCommandInfo("removeReward [Name] [Index]", "Removes a reward by index"));
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							int index;
							try
							{
								index = Integer.parseInt(args[2]);
								index--;
							}
							catch(NumberFormatException e)
							{
								sender.sendMessage(ChatColor.RED + "The index has to be a whole number!");
								return true;
							}
							if(index >= 0)
							{
								List<ItemStack> rewards = area.getRewards();
								if(index < rewards.size())
								{
									String type = rewards.get(index).getType().name();
									area.removeReward(index);
									saveArea(area);
									sender.sendMessage(ChatColor.GREEN + "Successfully removed a reward! (" + type + ")");
								}
								else
								{
									int max = rewards.size() + 1;
									sender.sendMessage(ChatColor.RED + "Reward not found, the index has to be smaller than " + max + ".");
								}
							}
							else
								sender.sendMessage(ChatColor.RED + "The index has to be larger than 0!");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("addMessage"))
				{ // MESSAGES
					if(args.length <= 2)
						sender.sendMessage(getCommandInfo("addMessage [Name] [Message]", "Adds a message, that shows, when a player reaches the area"));
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							String message = args[2];
							for(int i = 0; i < args.length - 3; i++)
								message = message + " " + args[i + 3];
							message = replaceColorKeys(message);
							area.addMessage(message);
							saveArea(area);
							String check = message;
							if(check.length() > 8)
								check = check.substring(0, 7) + "...";
							sender.sendMessage(ChatColor.GREEN + "Successfully added a message! (" + check + ChatColor.GREEN + ")");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("removeMessage"))
				{
					if(args.length <= 2)
						sender.sendMessage(getCommandInfo("removeMessage [Name] [Index]", "Removes a message by index"));
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							int index;
							try
							{
								index = Integer.parseInt(args[2]);
								index--;
							}
							catch(NumberFormatException e)
							{
								sender.sendMessage(ChatColor.RED + "The index has to be a whole number!");
								return true;
							}
							if(index >= 0)
							{
								List<String> messages = area.getMessages();
								if(index < messages.size())
								{
									String message = messages.get(index);
									if(message.length() > 8)
										message = message.substring(0, 7) + "...";
									area.removeMessage(index);
									saveArea(area);
									sender.sendMessage(ChatColor.GREEN + "Successfully removed a message! (" + message + ChatColor.GREEN + ")");
								}
								else
								{
									int max = messages.size() + 1;
									sender.sendMessage(ChatColor.RED + "Message not found, the index has to be smaller than " + max + ".");
								}
							}
							else
								sender.sendMessage(ChatColor.RED + "The index has to be larger than 0!");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("addCommand"))
				{ // COMMANDS
					if(args.length <= 2)
					{
						sender.sendMessage(getCommandInfo("addCommand [Name] [Command]", "Adds a console command, that fires when a player enters the area"));
						sender.sendMessage(ChatColor.GRAY + "Use %PLAYER% for player's name.");
					}
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							String command = args[2];
							for(int i = 0; i < args.length - 3; i++)
								command = command + " " + args[i + 3];
							area.addCommand(command);
							saveArea(area);
							String check = command;
							if(check.length() > 8)
								check = check.substring(0, 7) + "...";
							sender.sendMessage(ChatColor.GREEN + "Successfully added a command! (" + check + ChatColor.GREEN + ")");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("removeCommand"))
				{
					if(args.length <= 2)
						sender.sendMessage(getCommandInfo("removeCommands [Name] [Index]", "Removes a command by index"));
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							int index;
							try
							{
								index = Integer.parseInt(args[2]);
								index--;
							}
							catch(NumberFormatException e)
							{
								sender.sendMessage(ChatColor.RED + "The index has to be a whole number!");
								return true;
							}
							if(index >= 0)
							{
								List<String> commands = area.getCommands();
								if(index < commands.size())
								{
									String command = commands.get(index);
									if(command.length() > 8)
										command = command.substring(0, 7) + "...";
									area.removeCommand(index);
									saveArea(area);
									sender.sendMessage(ChatColor.GREEN + "Successfully removed a command! (" + command + ChatColor.GREEN + ")");
								}
								else
								{
									int max = commands.size() + 1;
									sender.sendMessage(ChatColor.RED + "Command not found, the index has to be smaller than " + max + ".");
								}
							}
							else
								sender.sendMessage(ChatColor.RED + "The index has to be larger than 0!");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("list"))
				{
					sender.sendMessage(ChatColor.AQUA + "Points of Interests created:");
					String string = "";
					for(POIArea area : areas.values())
						string += ", " + area.getName();
					if(string.length() > 0)
					{
						string = string.substring(2);
						sender.sendMessage(string);
					}
					else
						sender.sendMessage(ChatColor.RED + "No areas created! Use '/poi create'.");
					sender.sendMessage(ChatColor.AQUA + "---");
				}
				else if(args[0].equalsIgnoreCase("info"))
				{
					if(args.length <= 1)
						sender.sendMessage(getCommandInfo("info [Name]", "Shows information about specified area"));
					else
					{
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							Location pos1 = area.getFirstPosition();
							Location pos2 = area.getSecondPosition();
							String name = area.getName();
							int exp = area.getExperience();
							List<ItemStack> rewards = area.getRewards();
							List<String> messages = area.getMessages();
							List<String> commands = area.getCommands();
							sender.sendMessage(ChatColor.GOLD + "Area '" + name + "' info:");
							sender.sendMessage(ChatColor.AQUA + "First Position:" + ChatColor.GRAY + " x" + pos1.getBlockX() + " y" + pos1.getBlockY() + " z" + pos1.getBlockZ());
							sender.sendMessage(ChatColor.AQUA + "Second Position:" + ChatColor.GRAY + " x" + pos2.getBlockX() + " y" + pos2.getBlockY() + " z" + pos2.getBlockZ());
							if(exp != 0)
								sender.sendMessage(ChatColor.AQUA + "Experience: " + ChatColor.GRAY + exp);
							else
								sender.sendMessage(ChatColor.AQUA + "Experience: " + ChatColor.GRAY + "No experince set.");
							if(rewards.size() > 0)
							{
								sender.sendMessage(ChatColor.AQUA + "Rewards:");
								for(ItemStack reward : rewards)
								{
									int id = reward.getTypeId();
									String type = reward.getType().name();
									int data = reward.getDurability();
									int amount = reward.getAmount();
									String string = type + " (" + id + ")";
									if(data != 0)
										string += " Data: " + data;
									if(amount != 1)
										string += " Amount: " + amount;
									sender.sendMessage(ChatColor.GRAY + "  - " + string);
								}
							}
							else
								sender.sendMessage(ChatColor.AQUA + "Rewards: " + ChatColor.GRAY + "No rewards.");
							if(messages.size() > 0)
							{
								sender.sendMessage(ChatColor.AQUA + "Messages:");
								for(String message : messages)
									sender.sendMessage(ChatColor.GRAY + "  - '" + ChatColor.RESET + message + ChatColor.GRAY + "'");
							}
							else
								sender.sendMessage(ChatColor.AQUA + "Messages: " + ChatColor.GRAY + "No messages.");
							if(commands.size() > 0)
							{
								sender.sendMessage(ChatColor.AQUA + "Commands:");
								for(String command : commands)
									sender.sendMessage(ChatColor.GRAY + "  - '" + ChatColor.GREEN + "/" + command + ChatColor.GRAY + "'");
							}
							else
								sender.sendMessage(ChatColor.AQUA + "Commands: " + ChatColor.GRAY + "No commands.");
						}
						else
							sender.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else if(args[0].equalsIgnoreCase("tp"))
				{
					if(!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "Only for players! Sorry, console.");
						return true;
					}
					if(args.length <= 1)
						sender.sendMessage(getCommandInfo("tp [Name]", "Teleports you to the area's first position"));
					else
					{
						Player player = (Player) sender;
						POIArea area = getArea(args[1]);
						if(area != null)
						{
							player.teleport(area.getFirstPosition());
							player.sendMessage(ChatColor.GREEN + "Successfully teleported to area '" + args[1] + "'.");
						}
						else
							player.sendMessage(ChatColor.RED + "Area not found. :(");
					}
				}
				else
					sender.sendMessage(ChatColor.RED + "Invalid argument, use '/poi' with no arguments to list available arguments.");
			}
			else
				sender.sendMessage(ChatColor.RED + "You don't have permission.");
		return true;
	}

	public void info(String string)
	{
		getLogger().info(string);
	}

	public void warn(String string)
	{
		getLogger().warning("[Error]: " + string);
		for(Player player : Bukkit.getOnlinePlayers())
			if(player.isOp() || player.hasPermission(adminPermission))
				player.sendMessage(ChatColor.RED + "[POI Error]: " + string);
	}

	public POIArea createArea(String name, Location pos1, Location pos2)
	{
		int exp = 0;
		List<ItemStack> rewards = new ArrayList<ItemStack>();
		List<String> messages = new ArrayList<String>();
		List<String> commands = new ArrayList<String>();
		saveAreaWithArgs(name, pos1, pos2, exp, rewards, messages, commands);
		return addArea(name, pos1, pos2, exp, rewards, messages, commands);
	}

	public POIArea addArea(String name, Location pos1, Location pos2, int exp, List<ItemStack> rewards, List<String> messages, List<String> commands)
	{
		POIArea area = new POIArea(name, pos1, pos2);
		area.setExperience(exp);
		for(ItemStack reward : rewards)
			area.addReward(reward);
		for(String message : messages)
			area.addMessage(message);
		for(String command : commands)
			area.addCommand(command);
		areas.put(name, area);
		return area;
	}

	public void saveAreaWithArgs(String name, Location pos1, Location pos2, int exp, List<ItemStack> rawRewards, List<String> messages, List<String> commands)
	{
		String string1 = pos1.getWorld().getName() + "|" + pos1.getX() + "|" + pos1.getY() + "|" + pos1.getZ();
		String string2 = pos2.getWorld().getName() + "|" + pos2.getX() + "|" + pos2.getY() + "|" + pos2.getZ();
		List<String> rewards = new ArrayList<String>();
		for(ItemStack rawReward : rawRewards)
		{
			String reward = "";
			reward += rawReward.getTypeId() + "|";
			reward += rawReward.getDurability() + "|";
			reward += rawReward.getAmount();
			rewards.add(reward);
		}
		try
		{
			yamlAreas.set(name + ".loc", string1 + "||" + string2);
			yamlAreas.set(name + ".exp", exp);
			if(rewards.size() > 0)
				yamlAreas.set(name + ".rew", rewards);
			if(messages.size() > 0)
				yamlAreas.set(name + ".mes", messages);
			if(commands.size() > 0)
				yamlAreas.set(name + ".com", commands);
			yamlAreas.save(areasFile);
		}
		catch(Exception e)
		{
			warn("Saving area '" + name + "'");
			e.printStackTrace();
		}
	}

	public void saveArea(POIArea area)
	{
		String name = area.getName();
		Location pos1 = area.getFirstPosition();
		Location pos2 = area.getSecondPosition();
		int exp = area.getExperience();
		List<ItemStack> rewards = area.getRewards();
		List<String> messages = area.getMessages();
		List<String> commands = area.getCommands();
		saveAreaWithArgs(name, pos1, pos2, exp, rewards, messages, commands);
	}

	public String removeArea(String name)
	{
		for(POIArea area : areas.values())
		{
			String areaName = area.getName();
			if(areaName.equals(name))
			{
				yamlAreas.set(areaName, null);
				try
				{
					yamlAreas.save(areasFile);
				}
				catch(IOException e)
				{
					warn("Saving 'areas.yml'");
					e.printStackTrace();
				}
				areas.remove(name);
				return areaName;
			}
		}
		return null;
	}

	public void saveAreas()
	{
		for(POIArea area : areas.values())
			saveArea(area);
	}

	public void loadAreas()
	{
		if(!areasFile.exists())
		{
			info("'areas.yml' does not exist, creating one.");
			try
			{
				yamlAreas.save(areasFile);
			}
			catch(IOException e)
			{
				warn("Creating 'areas.yml'");
				e.printStackTrace();
			}
			return;
		}
		else
			try
			{
				yamlAreas.load(areasFile);
			}
			catch(Exception e)
			{
				warn("Loading 'areas.yml'");
			}
		for(String name : yamlAreas.getKeys(false))
			loadArea(name);
	}

	public POIArea loadArea(String name)
	{
		try
		{
			String raw = yamlAreas.getString(name + ".loc");
			List<String> strRewards = yamlAreas.getStringList(name + ".rew");
			List<ItemStack> rewards = new ArrayList<ItemStack>();
			for(String rawReward : strRewards)
			{
				String[] strReward = rawReward.split("\\|");
				int id, data, amount;
				id = Integer.parseInt(strReward[0]);
				if(strReward.length > 1)
				{
					data = Integer.parseInt(strReward[1]);
					if(strReward.length > 2)
						amount = Integer.parseInt(strReward[2]);
					else
						amount = 1;
				}
				else
				{
					data = 0;
					amount = 1;
				}
				ItemStack reward = new ItemStack(id, amount, (short) data);
				rewards.add(reward);
			}
			int exp = 0;
			if(yamlAreas.contains(name + ".exp"))
				exp = yamlAreas.getInt(name + ".exp");
			List<String> messages;
			if(yamlAreas.contains(name + ".mes"))
				messages = yamlAreas.getStringList(name + ".mes");
			else
				messages = new ArrayList<String>();
			List<String> commands;
			if(yamlAreas.contains(name + ".com"))
				commands = yamlAreas.getStringList(name + ".com");
			else
				commands = new ArrayList<String>();
			String[] raws = raw.split("\\|\\|");
			String[] firstRaws = raws[0].split("\\|");
			String[] secondRaws = raws[1].split("\\|");
			World[] world = { Bukkit.getWorld(firstRaws[0]), Bukkit.getWorld(secondRaws[0]) };
			double[] X = { Double.parseDouble(firstRaws[1]), Double.parseDouble(secondRaws[1]) };
			double[] Y = { Double.parseDouble(firstRaws[2]), Double.parseDouble(secondRaws[2]) };
			double[] Z = { Double.parseDouble(firstRaws[3]), Double.parseDouble(secondRaws[3]) };
			Location pos1 = new Location(world[0], X[0], Y[0], Z[0]);
			Location pos2 = new Location(world[1], X[1], Y[1], Z[1]);
			addArea(name, pos1, pos2, exp, rewards, messages, commands);
		}
		catch(Exception e)
		{
			warn("Loading area '" + name + "'");
			e.printStackTrace();
		}
		return null;
	}

	public POIArea getArea(String name)
	{
		for(POIArea area : areas.values())
			if(area.getName().equals(name))
				return area;
		return null;
	}

	public void listCommands(CommandSender sender)
	{
		sender.sendMessage(ChatColor.GOLD + "PointsOfInterests commands:");
		sender.sendMessage(getCommandInfo("pos1", "Sets the point of a cuboid area"));
		sender.sendMessage(getCommandInfo("pos2", "Sets the point of a cuboid area"));
		sender.sendMessage(getCommandInfo("create [Name] (RewardID) (Amount)", "Creates a point of interests"));
		sender.sendMessage(getCommandInfo("remove [Name]", "Removes a point of interests"));
		sender.sendMessage(getCommandInfo("setExperience [Name] [Experience]", "Assigns experience points to a point of interests"));
		sender.sendMessage(getCommandInfo("addReward [Name] [ItemID] (Data) (Amount)", "Assigns a reward to a point of interests"));
		sender.sendMessage(getCommandInfo("removeReward [Name] [Index]", "Removes a reward by index"));
		sender.sendMessage(getCommandInfo("addMessage [Name] [Message]", "Adds a message, that shows, when a player reaches the area"));
		sender.sendMessage(getCommandInfo("removeMessage [Name] [Index]", "Removes a message by index"));
		sender.sendMessage(getCommandInfo("addCommand [Name] [Command]", "Adds a console command, that fires when a player enters the area"));
		sender.sendMessage(getCommandInfo("removeCommands [Name] [Index]", "Removes a command by index"));
		sender.sendMessage(getCommandInfo("list", "Shows all areas"));
		sender.sendMessage(getCommandInfo("info [Name]", "Shows information about specified area"));
		sender.sendMessage(getCommandInfo("tp [Name]", "Teleports you to the area's first position"));
	}

	public String getCommandInfo(String command, String info)
	{
		return ChatColor.AQUA + "/poi " + command + "   - " + ChatColor.GRAY + info;
	}

	public void visit(POIArea area, Player player)
	{
		Location playerLoc = player.getLocation();
		World world = playerLoc.getWorld();
		int exp = area.getExperience();
		List<ItemStack> rewards = area.getRewards();
		List<String> commands = area.getCommands();
		String areaName = area.getName();
		String playerName = player.getName();
		List<String> visitors;
		Server server = getServer();
		ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();
		player.giveExp(exp);
		for(ItemStack reward : rewards)
		{
			Item item = world.dropItemNaturally(playerLoc, reward);
			Entity entity = item;
			entity.setVelocity(new Vector(0, 0, 0));
		}
		for(String command : commands)
		{
			command = command.replaceAll("%PLAYER%", playerName);
			server.dispatchCommand(consoleSender, command);
		}
		world.playSound(playerLoc, Sound.BLOCK_LAVA_POP, 10, 10);
		world.playSound(playerLoc, Sound.BLOCK_NOTE_BASS, 10, 10);
		if(yamlAreas.contains(areaName + ".vis"))
			visitors = yamlAreas.getStringList(areaName + ".vis");
		else
			visitors = new ArrayList<String>();
		visitors.add(playerName);
		yamlAreas.set(areaName + ".vis", visitors);
		try
		{
			yamlAreas.save(areasFile);
		}
		catch(Exception e)
		{
			warn("Saving 'areas.yml'");
			player.sendMessage(ChatColor.RED + "Something has gone wrong, tell the admin to check the console, please.");
			e.printStackTrace();
			return;
		}
		List<String> messages = area.getMessages();
		for(String message : messages)
			player.sendMessage(message);
	}

	public String replaceColorKeys(String string)
	{
		string = string.replaceAll("&0", ChatColor.BLACK + "");
		string = string.replaceAll("&1", ChatColor.DARK_BLUE + "");
		string = string.replaceAll("&2", ChatColor.DARK_GREEN + "");
		string = string.replaceAll("&3", ChatColor.DARK_AQUA + "");
		string = string.replaceAll("&4", ChatColor.DARK_RED + "");
		string = string.replaceAll("&5", ChatColor.DARK_PURPLE + "");
		string = string.replaceAll("&6", ChatColor.GOLD + "");
		string = string.replaceAll("&7", ChatColor.GRAY + "");
		string = string.replaceAll("&8", ChatColor.DARK_GRAY + "");
		string = string.replaceAll("&9", ChatColor.BLUE + "");
		string = string.replaceAll("&a", ChatColor.GREEN + "");
		string = string.replaceAll("&b", ChatColor.AQUA + "");
		string = string.replaceAll("&c", ChatColor.RED + "");
		string = string.replaceAll("&d", ChatColor.LIGHT_PURPLE + "");
		string = string.replaceAll("&e", ChatColor.YELLOW + "");
		string = string.replaceAll("&f", ChatColor.WHITE + "");
		string = string.replaceAll("&l", ChatColor.BOLD + "");
		string = string.replaceAll("&m", ChatColor.STRIKETHROUGH + "");
		string = string.replaceAll("&n", ChatColor.UNDERLINE + "");
		string = string.replaceAll("&o", ChatColor.ITALIC + "");
		return string;
	}

	public void metrics()
	{
		try
		{
			Metrics metrics = new Metrics(this);
			metrics.start();
		}
		catch(IOException e)
		{
			info("Plugin Metrics: Failed to submit stats");
		}
	}
}
