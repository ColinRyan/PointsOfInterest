package me.drbojingle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class POIArea
{
	Location pos1;
	Location pos2;
	String name;
	int exp;
	List<ItemStack> rewards = new ArrayList<ItemStack>();
	List<String> messages = new ArrayList<String>();
	List<String> commands = new ArrayList<String>();

	public POIArea(String name, Location pos1, Location pos2)
	{
		this.name = name;
		this.pos1 = pos1;
		this.pos2 = pos2;
	}

	public Location getFirstPosition()
	{
		return this.pos1;
	}

	public Location getSecondPosition()
	{
		return this.pos2;
	}

	public String getName()
	{
		return this.name;
	}

	public int getExperience()
	{
		return this.exp;
	}

	public List<ItemStack> getRewards()
	{
		return this.rewards;
	}

	public List<String> getMessages()
	{
		return this.messages;
	}

	public List<String> getCommands()
	{
		return this.commands;
	}

	public void setFirstPosition(Location pos1)
	{
		this.pos1 = pos1;
	}

	public void setSecondPosition(Location pos2)
	{
		this.pos2 = pos2;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setExperience(int exp)
	{
		this.exp = exp;
	}

	public void setRewards(List<ItemStack> rewards)
	{
		this.rewards = rewards;
	}

	public void setMessages(List<String> messages)
	{
		this.messages = messages;
	}

	public void setCommands(List<String> commands)
	{
		this.commands = commands;
	}

	public void addReward(ItemStack reward)
	{
		this.rewards.add(reward);
	}

	public void addMessage(String message)
	{
		this.messages.add(message);
	}

	public void addCommand(String command)
	{
		this.commands.add(command);
	}

	public void setReward(int index, ItemStack reward)
	{
		this.rewards.set(index, reward);
	}

	public void setMessage(int index, String message)
	{
		this.messages.set(index, message);
	}

	public void setCommand(int index, String command)
	{
		this.commands.set(index, command);
	}

	public void removeReward(int index)
	{
		this.rewards.remove(index);
	}

	public void removeMessage(int index)
	{
		this.messages.remove(index);
	}

	public void removeCommand(int index)
	{
		this.commands.remove(index);
	}

	public boolean hasInside(Location loc)
	{
		double[] dim = new double[2];
		dim[0] = this.pos1.getX();
		dim[1] = this.pos2.getX();
		Arrays.sort(dim);
		if(loc.getX() > dim[1] || loc.getX() < dim[0])
			return false;
		dim[0] = this.pos1.getY();
		dim[1] = this.pos2.getY();
		Arrays.sort(dim);
		if(loc.getY() > dim[1] || loc.getY() < dim[0])
			return false;
		dim[0] = this.pos1.getZ();
		dim[1] = this.pos2.getZ();
		Arrays.sort(dim);
		if(loc.getZ() > dim[1] || loc.getZ() < dim[0])
			return false;
		return true;
	}
}
