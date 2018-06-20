package at.mcnetwork.lausi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Configuration helper for HCP
 *
 * @author jascotty2
 */
public class Config {

	final static String UPDATE_SETTING_PATH = "update-notification";
	final HideAndCustomPlugins plugin;
	// internal settings
	private boolean dirty = false;
	BukkitTask delaySaveTaskId = null;
	protected Runnable delaySaveTask = new Runnable() {
		@Override
		public void run() {
			_save();
			delaySaveTaskId = null;
		}
	};

	// Cached configuration settings:
	boolean update_notify = true;
	public final ArrayList<String> blacklist = new ArrayList<String>();
	public final ArrayList<String> whitelist = new ArrayList<String>();
	public final ArrayList<String> fake_plugins = new ArrayList<String>();
	public final ArrayList<String> plugin_blacklist = new ArrayList<String>();
	String msg_error;
	boolean pot_enabled;
	String pot_effect_name, pot_sound_name;
	Sound pot_sound = null;
	PotionEffectType pot_type = null;
	int pot_time;
	boolean help_replace = false;
	boolean hide_commands_nopermission = true;
	boolean check_aliases = true;
	public final ArrayList<List<String>> help_pages = new ArrayList<List<String>>();

	protected static HashMap<String, Sound> soundAliases = new HashMap<String, Sound>() {{
			put("ENDERMEN", Sound.ENTITY_ENDERMEN_SCREAM);
			put("BLAZE", Sound.ENTITY_BLAZE_DEATH);
			put("ENDERDRAGON", Sound.ENTITY_ENDERDRAGON_DEATH);
			put("GHAST", Sound.ENTITY_GHAST_DEATH);
			put("GUARDIAN", Sound.ENTITY_ELDER_GUARDIAN_DEATH);
	}};

	protected ArrayList<String> helpBlacklist = new ArrayList<String>() {{
			addAll(Arrays.asList("?", "bukkit:?", "help", "bukkit:help", "minecraft:help"));
	}};
	protected ArrayList<String> pluginlistBlacklist = new ArrayList<String>() {{
			addAll(Arrays.asList("bukkit:pl", "bukkit:plugins", "pl", "plugins"));
	}};

	public Config(HideAndCustomPlugins plugin) {
		this.plugin = plugin;
	}

	public void reload() {
		plugin.reloadConfig();
		load();
	}

	public void saveNow() {
		if (delaySaveTaskId != null) {
			plugin.getServer().getScheduler().cancelTask(delaySaveTaskId.getTaskId());
			delaySaveTaskId = null;
		}
		_save();
	}

	protected void _save() {
		if (dirty) {
			dirty = false;
			// note: variable comments are overwritten by this api..
			// may want to separate editable options into another file if it's an issue
			// (or use a different library)
			FileConfiguration cfg = plugin.getConfig();
			synchronized (blacklist) {
				cfg.set("blocked-cmds", blacklist);
			}
			synchronized (whitelist) {
				cfg.set("allowed-cmds", whitelist);
			}
			synchronized (plugin_blacklist) {
				cfg.set("blocked-plugins", plugin_blacklist);
			}
			plugin.saveConfig();
		}
	}

	public boolean save() {
		// assuming we need to save
		dirty = true;
		if (delaySaveTaskId == null) {
			delaySaveTaskId = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, delaySaveTask, 20 * 30);
		}
		return true;
	}

	public void load() {
		// if the config file doesn't exist, create it
		plugin.saveDefaultConfig();
		// clear out any lingering settings
		blacklist.clear();
		whitelist.clear();
		help_pages.clear();
		fake_plugins.clear();
		dirty = false;
		// use bukkit's config loader
		FileConfiguration cfg = plugin.getConfig();
		cfg.options().copyDefaults(true);
		cfg.options().copyHeader(true);

		// copy values to local variables to optimise lookups
		update_notify = cfg.getBoolean(UPDATE_SETTING_PATH);
		// deprecating this config, since it's better to expose the setting to the server op
		if (cfg.contains("disable-messages")) {
			if(cfg.getBoolean("disable-messages")) {
				for (List<String> commandList : new List[]{
					Arrays.asList("bukkit:ver", "bukkit:version", "icanhasbukkit", "ver", "version", "about", "bukkit:about"),
					pluginlistBlacklist}) {
					for (String c : commandList) {
						addToBlacklist(c, false);
					}
				}
			}
			cfg.set("disable-messages", null);
			dirty = true;
		}

		msg_error = cfg.getString("error-message", "Unknown command. Type \"/help\" for help.");

		ConfigurationSection sec = cfg.getConfigurationSection("potions-on-plugin-command");
		// Changing the hiarchy of the potion config
		if (cfg.contains("use-potions")) {
			sec.set("enabled", cfg.getBoolean("use-potions", true));
			sec.set("effect", cfg.getString("effect", sec.getString("effect", "confusion")));
			sec.set("time", cfg.getInt("time", sec.getInt("time", 120)));
			sec.set("sound", cfg.getString("sound", sec.getString("sound", "blaze")));
			cfg.set("use-potions", null);
			cfg.set("effect", null);
			cfg.set("time", null);
			cfg.set("sound", null);
			dirty = true;
		}

		pot_enabled = sec.getBoolean("enabled", pot_enabled);
		pot_effect_name = sec.getString("effect", "none");
		pot_sound_name = sec.getString("sound", "none");
		pot_time = sec.getInt("time", pot_time);
		if (pot_effect_name != null && !pot_effect_name.equalsIgnoreCase("none")
				&& (pot_type = PotionEffectType.getByName(pot_effect_name = pot_effect_name.toUpperCase())) == null) {
			plugin.getLogger().info(String.format("[%s] Config Error: No known potion type \"%s\"", plugin.name, pot_effect_name));
		}
		// no convenient getter for sounds
		// first: alias sounds?
		if (pot_sound_name != null && !pot_sound_name.equalsIgnoreCase("none")
				&& (pot_sound = soundAliases.get(pot_sound_name = pot_sound_name.toUpperCase())) == null) {
			// not an alias, is this another sound?
			for (Sound s : Sound.values()) {
				if (pot_sound_name.equals(s.name())) {
					pot_sound = s;
					break;
				}
			}
			if (pot_sound == null) {
				plugin.getLogger().info(String.format("[%s] Config Error: No known sound \"%s\"", plugin.name, pot_sound_name));
			}
		}

		// also changing this command to replace rather than block. Command can be blocked in the blacklist section
		if (cfg.contains("disable-help-command")) {
			if (cfg.getBoolean("disable-help-command", false)) {
				for (String c : helpBlacklist) {
					addToBlacklist(c, false);
				}
			}
			cfg.set("disable-help-command", null);
			dirty = true;
		}
		
		help_replace = cfg.getBoolean("replace-help", help_replace);
		sec = cfg.getConfigurationSection("help-pages");
		for (String k : sec.getKeys(false)) {
			List l = sec.getList(k);
			if (l != null) {
				help_pages.add(l);
			}
		}

		fake_plugins.addAll(Arrays.asList(cfg.getString("plugins", "").split(", ")));
		// blacklist.addAll(cfg.getStringList("blocked-cmds"));
		// changing case, in the event that an admin adds uppercase characters in the config file
		for (String c : cfg.getStringList("blocked-cmds")) {
			addToBlacklist(c, false);
		}
		for (String c : cfg.getStringList("allowed-cmds")) {
			addToWhitelist(c, false);
		}
		// these lists must always be sorted
		Collections.sort(blacklist);
		Collections.sort(whitelist);

		check_aliases = cfg.getBoolean("auto-lookup-command-aliases");
		hide_commands_nopermission = cfg.getBoolean("auto-block-no-permissions");
		for (String c : cfg.getStringList("blocked-plugins")) {
			plugin_blacklist.add(c.toLowerCase());
		}

		// if there were any changes, they need to be saved
		saveNow();
	}

	protected boolean editList(String[] args, int start, List<String> list, boolean add) {
		if (args != null && args.length > start) {
			StringBuilder c = new StringBuilder();
			for (int i = start; i < args.length; ++i) {
				c.append(args[i]);
				if (i + 1 < args.length) {
					c.append(" ");
				}
			}
			// are we checking for alias commands?
			if (check_aliases) {
				PluginCommand pcmd = Bukkit.getServer().getPluginCommand(args[start]);
				if (pcmd != null && !args[start].equalsIgnoreCase(pcmd.getName())) {
					String cmdArgs = args.length > start + 1 ? "" : c.substring(args[start].length());
					// if this is a remove, remove both if there are both (just in case)
					if (!add && list.contains(args[start] + cmdArgs)) {
						list.remove(args[start] + cmdArgs);
						if (!list.contains(args[start] = pcmd.getName().toLowerCase() + cmdArgs)) {
							save();
							return true;
						}
					}
					// process with this as the command
					args[start] = pcmd.getName().toLowerCase() + cmdArgs;
				} else {
					args[start] = c.toString();
				}
			} else {
				// modify the arg array so the calling function doesn't need to concatenate anything
				args[start] = c.toString();
			}

			if (add && !list.contains(args[start])) {
				list.add(args[start]);
				// sort & save changes
				Collections.sort(list);
				save();
			} else if (!add && list.contains(args[start])) {
				list.remove(args[start]);
				save();
			} else {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Add everything in the arguments list after {@code start} as one command
	 *
	 * @param args
	 * @param start
	 * @return true if the command was added without duplicates <br>
	 * {@code args[start]} will have the concatenated command
	 */
	public boolean addToBlacklist(String[] args, int start) {
		// remove from whitelist if adding to blacklist
		if (editList(args, start, blacklist, true)) {
			removeFromWhitelist(args[start]);
			return true;
		}
		return false;
	}

	/**
	 * Remove a command from the blocked commands list using arguments after
	 * {@code start} as one command
	 *
	 * @param args
	 * @param start
	 * @return true if the command existed and was removed <br>
	 * {@code args[start]} will have the concatenated command
	 */
	public boolean removeFromBlacklist(String[] args, int start) {
		return editList(args, start, blacklist, false);
	}

	/**
	 * Add a plugin to the plugin blacklist
	 *
	 * @param pluginName
	 * @return true if the plugin was added without duplicates
	 */
	public boolean addToPluginBlacklist(String pluginName) {
		if(!plugin_blacklist.contains(pluginName = pluginName.toLowerCase())) {
			plugin_blacklist.add(pluginName);
			save();
			return true;
		}
		return false;
	}

	/**
	 * Remove a command from the blocked commands list using arguments after
	 * {@code start} as one command
	 *
	 * @param pluginName
	 * @return true if the command existed and was removed <br>
	 * {@code args[start]} will have the concatenated command
	 */
	public boolean removeFromPluginBlacklist(String pluginName) {
		return plugin_blacklist.remove(pluginName.toLowerCase()) && save();
	}
	
	/**
	 * Add everything in the arguments list after {@code start} as one command
	 *
	 * @param args
	 * @param start
	 * @return true if the command was added without duplicates <br>
	 * {@code args[start]} will have the concatenated command
	 */
	public boolean addToWhitelist(String[] args, int start) {
		// remove from blacklist if adding to whitelist
		if (editList(args, start, whitelist, true)) {
			removeFromBlacklist(args[start]);
			return true;
		}
		return false;
	}

	/**
	 * Remove a command from the blocked commands list using arguments after
	 * {@code start} as one command
	 *
	 * @param args
	 * @param start
	 * @return true if the command existed and was removed <br>
	 * {@code args[start]} will have the concatenated command
	 */
	public boolean removeFromWhitelist(String[] args, int start) {
		return editList(args, start, whitelist, false);
	}

	/**
	 * Add a command to the blocked commands
	 *
	 * @param command
	 * @param resort set to false ONLY if you are going to manually sort
	 * afterwards
	 * @return true if the command was added without duplicates
	 */
	public boolean addToBlacklist(String command, boolean resort) {
		if ((command = command.toLowerCase()).startsWith("/")) {
			command = command.substring(1);
		}
		removeFromWhitelist(command);
		if (!blacklist.contains(command) && blacklist.add(command)) {
			if (resort) {
				Collections.sort(blacklist);
			}
			save();
			return true;
		}
		return false;
	}

	/**
	 * Remove a command from the blocked commands list
	 *
	 * @param command
	 * @return true if the command was in the list and was removed
	 */
	public boolean removeFromBlacklist(String command) {
		return blacklist.remove(command.toLowerCase())
				// save changes
				&& save();
	}

	/**
	 * Add a command to the allowed commands
	 *
	 * @param command
	 * @param resort set to false ONLY if you are going to manually sort
	 * @return true if the command was added without duplicates
	 */
	public boolean addToWhitelist(String command, boolean resort) {
		if ((command = command.toLowerCase()).startsWith("/")) {
			command = command.substring(1);
		}
		if (!whitelist.contains(command) && whitelist.add(command)) {
			if (resort) {
				Collections.sort(whitelist);
			}
			save();
			return true;
		}
		return false;
	}

	/**
	 * Remove a command from the allowed commands list
	 *
	 * @param command
	 * @return true if the command was in the list and was removed
	 */
	public boolean removeFromWhitelist(String command) {
		return whitelist.remove(command.toLowerCase())
				// save changes
				&& save();
	}

	/**
	 * Check to see if a command is blacklisted.
	 *
	 * @param command
	 * @return true if this is blacklisted
	 */
	public boolean isBlacklisted(String command) {
		int k = Collections.binarySearch(blacklist, command = command.toLowerCase());
		// if not found, k = -(insertion point) - 1
		while (k < 0 && command.contains(" ")) {
			command = command.substring(0, command.lastIndexOf(' '));
			k = Collections.binarySearch(blacklist, command = command.toLowerCase());
		}
		return k >= 0;
	}

	/**
	 * Check to see if a command is whitelisted.
	 *
	 * @param command
	 * @return true if this is blacklisted
	 */
	public boolean isWhitelisted(String command) {
		int k = Collections.binarySearch(whitelist, command = command.toLowerCase());
		// if not found, k = -(insertion point) - 1
		while (k < 0 && command.contains(" ")) {
			command = command.substring(0, command.lastIndexOf(' '));
			k = Collections.binarySearch(whitelist, command = command.toLowerCase());
		}
		return k >= 0;
	}

	public boolean isPluginlistCommand(String command) {
		if ((command = command.toLowerCase()).contains(" ")) {
			// nice try ;)
			command = command.substring(0, command.indexOf(' '));
		}
		return pluginlistBlacklist.contains(command);
	}

	public boolean isHelpCommand(String command) {
		if ((command = command.toLowerCase()).contains(" ")) {
			command = command.substring(0, command.indexOf(' '));
		}
		return helpBlacklist.contains(command);
	}

}
