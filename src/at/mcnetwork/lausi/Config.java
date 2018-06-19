package at.mcnetwork.lausi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Configuration helper for HCP
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
	boolean block_builtin = false;
	public final ArrayList<String> plugins = new ArrayList<String>();
	public final ArrayList<String> blacklist = new ArrayList<String>();
	String msg_error;
	boolean pot_enabled;
	String pot_effect_name, pot_sound_name;
	Sound pot_sound = null;
	PotionEffectType pot_type = null;
	int pot_time;
	boolean help_disabled = false;
	
	protected static HashMap<String, Sound> soundAliases = new HashMap<String, Sound>(){{
		put("ENDERMEN", Sound.ENTITY_ENDERMEN_SCREAM);
		put("BLAZE", Sound.ENTITY_BLAZE_DEATH);
		put("ENDERDRAGON", Sound.ENTITY_ENDERDRAGON_DEATH);
		put("GHAST", Sound.ENTITY_GHAST_DEATH);
		put("GUARDIAN", Sound.ENTITY_ELDER_GUARDIAN_DEATH);
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
		if(dirty) {
			dirty = false;
			// note: variable comments are overwritten by this api..
			// may want to separate editable options into another file if it's an issue
			// (or use a different library)
			FileConfiguration cfg = plugin.getConfig();
			synchronized(blacklist) {
				cfg.set("blocked-cmds", blacklist);
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
		plugin.saveDefaultConfig();
		dirty = false;
		// use bukkit's config loader
		FileConfiguration cfg = plugin.getConfig();
		cfg.options().copyDefaults(true);
		cfg.options().copyHeader(true);
		
		// copy values to local variables to optimise lookups
		update_notify = cfg.getBoolean(UPDATE_SETTING_PATH);
		block_builtin = cfg.getBoolean("disable-messages");
		msg_error = cfg.getString("error-message");
		
		ConfigurationSection sec = cfg.getConfigurationSection("potions-on-plugin-command");
		// Changing the hiarchy of the potion config
		if(cfg.contains("use-potions")) {
			sec.set("enabled", cfg.getBoolean("use-potions", true));
			sec.set("effect", cfg.getString("effect", sec.getString("effect", "confusion")));
			sec.set("time", cfg.getInt("time", sec.getInt("time", 120)));
			sec.set("sound", cfg.getString("sound", sec.getString("sound", "blaze")));
			cfg.set("use-potions", null);
			cfg.set("effect", null);
			cfg.set("time", null);
			cfg.set("sound", null);
		}
		
		pot_enabled = sec.getBoolean("enabled");
		pot_effect_name = sec.getString("effect");
		pot_sound_name = sec.getString("sound");
		pot_time = sec.getInt("time");
		if(pot_effect_name != null && !pot_effect_name.equalsIgnoreCase("none")
				&& (pot_type = PotionEffectType.getByName(pot_effect_name = pot_effect_name.toUpperCase())) == null) {
			plugin.getLogger().info(String.format("[%s] Config Error: No known potion type \"%s\"", plugin.name, pot_effect_name));
		}
		// no convenient getter for sounds
		// first: alias sounds?
		if(pot_sound_name != null && !pot_sound_name.equalsIgnoreCase("none")
				&& (pot_sound = soundAliases.get(pot_sound_name = pot_sound_name.toUpperCase())) == null) {
			// not an alias, is this another sound?
			for(Sound s : Sound.values()) {
				if(pot_sound_name.equals(s.name())) {
					pot_sound = s;
					break;
				}
			}
			if(pot_sound == null) {
				plugin.getLogger().info(String.format("[%s] Config Error: No known sound \"%s\"", plugin.name, pot_sound_name));
			}
		}
		
		help_disabled = cfg.getBoolean("disable-help-command");
		
		plugins.addAll(Arrays.asList(cfg.getString("plugins").split(", ")));
		// blacklist.addAll(cfg.getStringList("blocked-cmds"));
		// changing case, in the event that an admin adds uppercase characters in the config file
		for(String s : cfg.getStringList("blocked-cmds")) {
			blacklist.add(s.toLowerCase());
		}
		// blacklist must always be sorted
		sortBlacklist();
	}
	
	private boolean sortBlacklist() {
		Collections.sort(blacklist);
		return true;
	}
	
	/**
	 * Add everything in the arguments list after {@code start} as one command
	 * @param args
	 * @param start
	 * @return true if the command was added without duplicates
	 */
	public boolean addToBlacklist(String[] args, int start) {
		if(args != null && args.length > start) {
			StringBuilder c = new StringBuilder();
			for(int i = start; i < args.length; ++i) {
				c.append(args[i]);
				if(i + 1 < args.length) {
					c.append(" ");
				}
			}
			return addToBlacklist(c.toString());
		}
		return false;
	}
	
	/**
	 * Add a command to the blocked commands
	 * @param command
	 * @return true if the command was added without duplicates
	 */
	public boolean addToBlacklist(String command) {
		if((command = command.toLowerCase()).startsWith("/")) {
			command = command.substring(1);
		}
		return !blacklist.contains(command) && blacklist.add(command)
				// sort & save changes
				&& sortBlacklist() && save();
	}
	
	/**
	 * Remove a command from the blocked commands list using arguments after {@code start} as one command
	 * @param args
	 * @param start
	 * @return true if the command was added without duplicates
	 */
	public boolean removeFromBlacklist(String[] args, int start) {
		if(args != null && args.length > start) {
			StringBuilder c = new StringBuilder();
			for(int i = start; i < args.length; ++i) {
				c.append(args[i]);
				if(i + 1 < args.length) {
					c.append(" ");
				}
			}
			return removeFromBlacklist(c.toString());
		}
		return false;
	}
	
	/**
	 * Remove a command from the blocked commands list
	 * @param command
	 * @return true if the command was in the list and was removed
	 */
	public boolean removeFromBlacklist(String command) {
		if(command.startsWith("/")) {
			command = command.substring(1);
		}
		return blacklist.remove(command.toLowerCase())
				// save changes
				&& save();
	}
	
	/**
	 * Check to see if a command is blacklisted.
	 * @param command
	 * @return true if this is blacklisted
	 */
	public boolean isBlacklisted(String command) {
		if((command = command.toLowerCase()).startsWith("/")) {
			command = command.substring(1);
		}
		int k = Collections.binarySearch(blacklist, command);
		// is this the result?
		String res = blacklist.get(k);
		if(command.startsWith(res)) {
			return true;
		}
		System.out.println("Not Found: " + command + " != " + res);
		return false;
	}

	
}
