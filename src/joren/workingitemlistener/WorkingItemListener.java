package joren.workingitemlistener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class WorkingItemListener extends JavaPlugin implements Listener {

	/** The logger */
	private static Logger log = Logger.getLogger("WorkingItemListener");

	/** Handle to access the Permissions plugin */
	public static PermissionHandler permissions;
	/** Name of the plugin, used in output messages */
	protected static String name = "WorkingItemListener";
	/** Path where the plugin's saved information is located */
	protected static String path = "plugins" + File.separator + name;
	/** Location of the config YML file */
	protected static String config = path + File.separator + name + ".yml";
	/** Header used for console and player output messages */
	protected static String header = "[" + name + "] ";
	protected static Map<String, List<Reward>> serverRewards = new HashMap<String, List<Reward>>();
	/** Represents the plugin's YML configuration */
	protected static FileConfiguration cfg = null;
	/**
	 * Instantiates a new listener.
	 */
	public void onEnable() {
		info ("Loading config...");
		try {
			File file = new File(config);
			if(!file.exists())
			{
				warning("Could not find a configuration file, saving a new one...");
				if (!saveDefault())
				{
					warning("Running on default values, but could not save a new configuration file.");
				}
			}
			else
			{
				cfg = YamlConfiguration.loadConfiguration(file);
				Set<String> serverSections = cfg.getKeys(false);
				for (Iterator<String> iter = serverSections.iterator(); iter.hasNext();) {
					String server = iter.next();
					info("Reading server: " + server);
					ConfigurationSection serverConfig = cfg.getConfigurationSection(server);
					Set<String> rewardSections = serverConfig.getKeys(false);
					List<Reward> rewards = new ArrayList<Reward>();
					for (Iterator<String> miniIter = rewardSections.iterator(); miniIter.hasNext();) {
						String itemName = miniIter.next();
						info("Reading item: " + server + "/" + itemName);
						ConfigurationSection itemConfig = serverConfig.getConfigurationSection(itemName);
						try {
							int item = Integer.valueOf(itemName);
							short damage = (short) itemConfig.getInt("damage", 0);
							info("Reading item damage: " + server + "/" + itemName + "/" + damage);
							int amount = itemConfig.getInt("amount", 1);
							info("Reading item amount: " + server + "/" + itemName + "/" + amount);
							Reward reward = new Reward(item, damage, amount);
							rewards.add(reward);
						} catch (NumberFormatException e) {
							//om nom nom
							System.out.println("Not a number: " + server + "/" + itemName);
						}
					}
					serverRewards.put(server, rewards);
				}
			}
			info("done.");
		} catch (Exception ex) {
			ex.printStackTrace();
			warning("Unable to load " + config + " due to " + ex.getClass().getSimpleName() + ", no rewards will be given and the plugin may blow up.");
		}
        getServer().getPluginManager().registerEvents(this, this);
	}
	
	/**
	 * Saves a new default configuration file, overwriting old configuration and file in the process
	 * Any existing configuration will be replaced with the default configuration and saved to disk.  Any variables that need to be read from the configuration will be initialized
	 * @return boolean: True if successful, false otherwise
	 */
	public boolean saveDefault()
	{
		cfg = new YamlConfiguration();
		info("Resetting configuration file with default values...");
		InputStream stream = getResource("WorkingItemListener.yml");
		if (stream != null)
		{
			cfg.setDefaults(YamlConfiguration.loadConfiguration(stream));
			cfg.options().copyDefaults(true);
		}
		else
		{
			severe("Did you delete the configuration yml from your jar file?  That was a really.  bad.  idea.  Go get yourself a new jar.");
			return false;
		}
		return save();
	}
	
	/**
	 * Saves the configuration file, overwriting old file in the process
	 * 
	 * @return boolean: True if successful, false otherwise.
	 */
	public boolean save()
	{
		info("Saving configuration file...");
		File dir = new File(path);
		if(!dir.exists())
		{
			if (!dir.mkdir())
			{
				severe("Could not create directory " + path + "; if there is a file with this name, please rename it to something else.  Please make sure the server has rights to make this directory.");
				return false;
			}
			info("Created directory " + path + "; this is where your configuration file will be kept.");
		}
		File file = new File(config);
		try
		{
			cfg.save(file);
		} catch (IOException e)
		{
			severe("Configuration could not be saved correctly(" + e.getLocalizedMessage() + ")! Please make sure the server has rights to output to " + config);
			return false;
		}
		info("Saved configuration file: " + config);
		return true;
	}


	@EventHandler(priority=EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent voteEvent) {
		Vote vote = voteEvent.getVote();
		String username = vote.getUsername();
		Player player = Bukkit.getServer().getPlayer(username);
		info("Received vote for " + vote.getUsername() + " at " + vote.getServiceName() + "(" + vote.getAddress() +")");
		if (player != null) {
			player.sendMessage("Thanks for voting on " + vote.getServiceName() + "!");
			info("Player is online");
			for (Iterator<Reward> i = serverRewards.get(vote.getServiceName()).iterator(); i.hasNext();)
			{
				Reward reward = i.next();
				info("Trying to give player " + reward.amount + " of " + reward.item + ":" + reward.damage + "...");
				ItemStack rewardStack = new ItemStack(reward.item, reward.amount, reward.damage);
				player.getWorld().dropItem(player.getLocation(), rewardStack);
				player.sendMessage("You just got rewarded with " + reward.amount + " " + rewardStack.getType().name() + ":" + reward.damage + "s!  (Check the ground below you in case your inventory was full)");
				player.sendMessage("..." + rewardStack.getType().name() + "(s) received.");
			}
		}
	}
	/**
	 * Logs an informative message to the console, prefaced with this plugin's header
	 * @param message: String
	 */
	protected static void info(String message)
	{
		log.info(header + message);
	}

	/**
	 * Logs a severe error message to the console, prefaced with this plugin's header
	 * Used to log severe problems that have prevented normal execution of the plugin
	 * @param message: String
	 */
	protected static void severe(String message)
	{
		log.severe(header + message);
	}

	/**
	 * Logs a warning message to the console, prefaced with this plugin's header
	 * Used to log problems that could interfere with the plugin's ability to meet admin expectations
	 * @param message: String
	 */
	protected static void warning(String message)
	{
		log.warning(header + message);
	}

	/**
	 * Logs a message to the console, prefaced with this plugin's header
	 * @param level: Logging level under which to send the message
	 * @param message: String
	 */
	protected static void log(java.util.logging.Level level, String message)
	{
		log.log(level, header + message);
	}
	
	private class Reward {
		private int item;
		private short damage;
		private int amount;
		
		Reward(int item, short damage, int amount) {
			this.item = item;
			this.damage = damage;
			this.amount = amount;
		}
	}

}
