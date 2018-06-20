package at.mcnetwork.lausi;

import java.io.IOException;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

/**
 *
 * @author Michael Lausegger | LauseggerDevelopment
 * @version 1.8.1
 * @since May 16, 2014
 *
 */
public class HideAndCustomPlugins extends JavaPlugin implements Listener {

	Config config = new Config(this);
	Updater updater = new Updater(this, 80016, "http://dev.bukkit.org/bukkit-plugins/hideandcustomplugins/", Config.UPDATE_SETTING_PATH);
	String version;
	String name;

	@Override
	public void onEnable() {
		version = getDescription().getVersion();
		name = getDescription().getName();

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
			getLogger().info("Metrics started: http://mcstats.org/plugin/HideAndCustomPlugins");
			new bstats(this);
			getLogger().info("Metrics started: https://bstats.org/plugin/bukkit/HideandCustomPlugins");
		} catch (IOException e) {
			getLogger().info("Error Submitting stats!");
		}

		config.load();

		if (config.update_notify) {
			updater.search();
		}

		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		tabHook();

		getLogger().info("Plugin has been activated successfully.");
	}

	/**
	 * Attempt to override the default tab-complete behavior
	 */
	void tabHook() {
		Plugin pl = getServer().getPluginManager().getPlugin("ProtocolLib");
		if (pl == null || !(pl instanceof com.comphenix.protocol.ProtocolLib)) {
			getLogger().warning("Failed to load ProtocolLib - tab completion behavior cannot be overriden!");
		} else {
			// Java witchcraft: offloading this to another class and only calling it if we know it can be called
			// (stops the NoClassDefFoundError that would happen otherwise)
			TabCompleteListener.installHook(this);
		}
	}

	@Override
	public void onDisable() {
		config.saveNow();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String command = event.getMessage().substring(1);

		// whitelist processes before any other checks
		if (player.hasPermission("hideandcustomplugins.bypass") || config.isWhitelisted(command)) {
			return;
		}

		if (config.isPluginlistCommand(command)) {
			// custom plugin list
			StringBuilder pl = new StringBuilder(ChatColor.GREEN.toString());
			for (String plugin : config.fake_plugins) {
				pl.append(plugin).append(ChatColor.WHITE).append(", ").append(ChatColor.GREEN);
			}
			player.sendMessage(ChatColor.WHITE + "Plugins (" + config.fake_plugins.size() + "): "
					+ pl.substring(0, pl.length() - 6));
			// apply potion effects, if enabled
			if (config.pot_enabled) {
				if (config.pot_type != null) {
					player.addPotionEffect(new PotionEffect(config.pot_type, config.pot_time * 20, 1));
				}
				if (config.pot_sound != null) {
					player.playSound(player.getLocation(), config.pot_sound, 2.0F, 1.0F);
				}
			}
			event.setCancelled(true);
		} else if (config.help_replace && config.isHelpCommand(command)) {
			int sp = command.indexOf(' ');
			onHelpCommand(player, sp == -1 ? null : command.substring(sp + 1).split(" "));
			event.setCancelled(true);
		} else if (config.isBlacklisted(command)) {
			getLogger().info(String.format("[%s] Blocked /%s (Command blacklisted)", name, command));
			player.sendMessage(config.msg_error.replace("&", "§"));
			event.setCancelled(true);
		} else {
			PluginCommand pcmd = Bukkit.getServer().getPluginCommand(command);
			if (pcmd != null) {
				// check if the base command is blacklisted
				if (config.check_aliases && !(command + " ").startsWith(pcmd.getName().toLowerCase() + " ")
						&& config.isBlacklisted(pcmd.getName() + (command.indexOf(' ') == -1 ? "" : command.substring(command.indexOf(' '))))) {
					getLogger().info(String.format("Blocked /%s (Command blacklisted)", command));
					player.sendMessage(config.msg_error.replace("&", "§"));
					event.setCancelled(true);
					return;
				}
				
				// is the plugin blacklisted?
				if (!player.hasPermission("hideandcustomplugins.plugin.*")
						&& !player.hasPermission("hideandcustomplugins.plugin." + pcmd.getPlugin().getName().toLowerCase())) {
					getLogger().info(String.format("Blocked /%s (Plugin blacklisted: %s)", command, pcmd.getPlugin().getName()));
					player.sendMessage(config.msg_error.replace("&", "§"));
					event.setCancelled(true);
					return;
				}
				
				// does the player have permission to use the command?
				if (config.hide_commands_nopermission && !pcmd.testPermissionSilent(player)) {
					getLogger().info(String.format("Blocked /%s (Permission fail: %s)", command, pcmd.getPermission()));
					player.sendMessage(config.msg_error.replace("&", "§"));
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (config.update_notify && player.hasPermission("hideandcustomplugins.bypass")) {
			updater.search(player);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			args = new String[]{"help"};
		}
		switch (args[0].toLowerCase()) {
			case "reload":
				if (sender.hasPermission("hideandcustomplugins.reload")) {
					if (args.length == 1) {
						config.reload();
						sender.sendMessage(ChatColor.GREEN + "Reloaded the config.yml of " + getDescription().getName() + " v" + getDescription().getVersion());
					} else {
						sender.sendMessage("§cToo many arguments!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.reload");
				}
				break;
				
				
			case "add":
			case "a":
				if (sender.hasPermission("hideandcustomplugins.add")) {
					if (args.length >= 2) {
						if (config.addToBlacklist(args, 1)) {
							sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.RED + "/" + args[1] + ChatColor.GREEN + " to the blacklist!");
						} else {
							sender.sendMessage(ChatColor.RED + "The Command " + ChatColor.YELLOW + "/" + args[1] + ChatColor.RED + " is already blocked!");
						}
					} else {
						sender.sendMessage("§cMissing command to add!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.remove");
				}
				break;
				
				
			case "remove":
			case "r":
				if (sender.hasPermission("hideandcustomplugins.remove")) {
					if (args.length >= 2) {
						if (config.removeFromBlacklist(args, 1)) {
							sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.RED + "/" + args[1] + ChatColor.GREEN + " from the blacklist!");
						} else {
							sender.sendMessage(ChatColor.RED + "The Command " + ChatColor.YELLOW + "/" + args[1] + ChatColor.RED + " is not blocked!");
						}
					} else {
						sender.sendMessage("§cMissing command to remove!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.add");
				}
				break;
				
				
			case "addplugin":
			case "ap":
				if (sender.hasPermission("hideandcustomplugins.add")) {
					if(args.length >= 2) { // we're just going to ignore extra arguments for now
						Plugin p = getServer().getPluginManager().getPlugin(args[1]);
						if(p == null) {
							// plugin manager is case-sensitive...
							for(Plugin pl : getServer().getPluginManager().getPlugins()) {
								if(pl != null && args[1].equalsIgnoreCase(pl.getName())) {
									p = pl;
									break;
								}
							}
						}
						if(p != null) {
							if (config.addToPluginBlacklist(p.getName())) {
								sender.sendMessage(ChatColor.GREEN + "Added Plugin " + ChatColor.RED + p.getName() + ChatColor.GREEN + " to the blacklist!");
							} else {
								sender.sendMessage(ChatColor.RED + "The Plugin " + ChatColor.YELLOW + p.getName() + ChatColor.RED + " is already blocked!");
							}
						} else {
							sender.sendMessage(ChatColor.RED + "The Plugin " + ChatColor.YELLOW + args[1] + ChatColor.RED + " was not found on the server!");
						}
					} else {
						sender.sendMessage("§cMissing plugin to add!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.remove");
				}
				break;
				
				
			case "removeplugin":
			case "rp":
				if (sender.hasPermission("hideandcustomplugins.remove")) {
					if (args.length >= 2) {
						if (config.removeFromPluginBlacklist(args[1])) {
							sender.sendMessage(ChatColor.GREEN + "Removed Plugin " + ChatColor.RED + args[1] + ChatColor.GREEN + " from the blacklist!");
						} else {
							sender.sendMessage(ChatColor.RED + "The Plugin " + ChatColor.YELLOW + args[1] + ChatColor.RED + " is not blocked!");
						}
					} else {
						sender.sendMessage("§cMissing plugin to remove!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.add");
				}
				break;
				
				
			case "blacklist":
				if (sender.hasPermission("hideandcustomplugins.blacklist")) {
					if (args.length == 1) {
						sender.sendMessage(ChatColor.GREEN + "List of blocked commands:");
						for (String list : config.blacklist) {
							sender.sendMessage(ChatColor.AQUA + "- " + list);
						}
					}
					if (args.length > 1) {
						sender.sendMessage("§cFalse or to many arguments!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.blacklist");
				}
				break;
				
				
			case "whitelist":
				if (sender.hasPermission("hideandcustomplugins.blacklist")) {
					if (args.length == 1) {
						sender.sendMessage(ChatColor.GREEN + "List of blocked commands:");
						for (String list : config.whitelist) {
							sender.sendMessage(ChatColor.AQUA + "- " + list);
						}
					}
					if (args.length > 1) {
						sender.sendMessage("§cFalse or to many arguments!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.blacklist");
				}
				break;
				
				
			case "addwhite":
			case "aw":
				if (sender.hasPermission("hideandcustomplugins.whitelist")) {
					if (args.length >= 2) {
						if (config.addToWhitelist(args, 1)) {
							sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.RED + "/" + args[1] + ChatColor.GREEN + " to the whitelist!");
						} else {
							sender.sendMessage(ChatColor.RED + "The Command " + ChatColor.YELLOW + "/" + args[1] + ChatColor.RED + " is already whitelisted!");
						}
					} else {
						sender.sendMessage("§cMissing command to add!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.whitelist");
				}
				break;
				
				
			case "removewhite":
			case "rw":
				if (sender.hasPermission("hideandcustomplugins.whitelist")) {
					if (args.length >= 2) {
						if (config.removeFromWhitelist(args, 1)) {
							sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.RED + "/" + args[1] + ChatColor.GREEN + " from the whitelist!");
						} else {
							sender.sendMessage(ChatColor.RED + "The Command " + ChatColor.YELLOW + "/" + args[1] + ChatColor.RED + " is not whitelisted!");
						}
					} else {
						sender.sendMessage("§cMissing command to remove!\n§a/hcp - Information about the plugin");
					}
				} else {
					sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.whitelist");
				}
				break;
				
				
			case "help":
			case "?":
			default:
				// no need to check if they have hideandcustomplugins.info - they can't run /hcp without it
				sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
				sender.sendMessage("§9/hcp reload - Reloads the config.yml.\n");
				sender.sendMessage("§9/hcp add <cmd> - Add a command to the blacklist.\n");
				sender.sendMessage("§9/hcp remove <cmd> - Remove a command from the blacklist.\n");
				sender.sendMessage("§9/hcp addwhite <cmd> - Add a command to the whitelist.\n");
				sender.sendMessage("§9/hcp removewhite <cmd> - Remove a command from the whitelist.\n");
				sender.sendMessage("§9/hcp addplugin <cmd> - Add a plugin to the blacklist.\n");
				sender.sendMessage("§9/hcp removeplugin <cmd> - Remove a plugin from the blacklist.\n");
				sender.sendMessage("§9/hcp blacklist - Shows a list with the blocked commands.\n");
				sender.sendMessage("§9/hcp whitelist - Shows a list with the allowed commands.\n");
				sender.sendMessage("§aHCP protects the server against pluginthieves");
				sender.sendMessage("§5Version: " + ChatColor.DARK_PURPLE + version);
				sender.sendMessage("§5Created by: " + ChatColor.DARK_PURPLE + "LauseggerDevelopment");
				sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
				break;
				
				//default:
				//sender.sendMessage("§cInvalid command.\n§a/hcp - Information about the plugin");
		}

		return true;
	}

	public void onHelpCommand(Player sender, String[] args) {
		int page = 1;
		String command = null;
		if (args != null) {
			if (args.length >= 2) {
				command = args[0];
				try {
					page = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					page = 1;
				}
			} else if (args.length == 1) {
				try {
					page = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					command = args[0];
					page = 1;
				}
			}
		}
		if (command != null) {
			// if requesting help for a command on the whitelist, show that command's useage
			boolean allow = config.isWhitelisted(command);
			// blacklisted commands cannot be executed, even if they have plugin perms
			if (!allow && !config.isBlacklisted(command)) {
				// does the user have access to the given plugin?
				PluginCommand pcmd = Bukkit.getServer().getPluginCommand(command);
				allow = pcmd != null
						&& (sender.hasPermission("hideandcustomplugins.plugin.*")
						|| !config.plugin_blacklist.contains(pcmd.getPlugin().getName().toLowerCase())
						|| sender.hasPermission("hideandcustomplugins.plugin." + pcmd.getPlugin().getName().toLowerCase()));
			}

			if (allow) {
				// send at least some message if there isn't any help
				allow = false;
				// help topics for commands start with /
				command = "/" + command;
				for (HelpTopic h : Bukkit.getHelpMap().getHelpTopics()) {
					if (h.getName().equalsIgnoreCase(command)) {
						String header = String.format("%s---------%s Help: %s %s",
								ChatColor.YELLOW, ChatColor.WHITE, command, ChatColor.YELLOW);
						sender.sendMessage(ChatStr.padRight(header, 45, '-'));
						sender.sendMessage(h.getFullText(sender));
						allow = true;
						break;
					}
				}
			}
			if (!allow) {
				sender.sendMessage("§cNo help for " + command);
			}
		} else {
			final int max = config.help_pages.size();
			if (page < 1) {
				page = 1;
			} else if (page > max) {
				page = max;
			}
			List<String> lines = config.help_pages.get(page - 1);
			String header = String.format("%s---------%s Help: Index (%d/%d) %s",
					ChatColor.YELLOW, ChatColor.WHITE, page, max, ChatColor.YELLOW);
			sender.sendMessage(ChatStr.padRight(header, 50, '-'));
			for (String l : lines) {
				sender.sendMessage(l.replace("&", "§"));
			}
		}
	}

}
