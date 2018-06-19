package at.mcnetwork.lausi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

/**
 *
 * @author Michael Lausegger | LauseggerDevelopment
 * @version 1.8.1
 * @since May 16, 2014
 *
 */
public class HideAndCustomPlugins extends JavaPlugin implements Listener {

	ProtocolManager protocolManager;
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
			getLogger().info("[" + name + "] Metrics started: http://mcstats.org/plugin/HideAndCustomPlugins");
			new bstats(this);
			getLogger().info("[" + name + "] Metrics started: https://bstats.org/plugin/bukkit/HideandCustomPlugins");
		} catch (IOException e) {
			getLogger().info("[" + name + "] Error Submitting stats!");
		}
		
		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		config.load();

		if (config.update_notify) {
			updater.search();
		}

		final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		manager.addPacketListener(new PacketAdapter(this, new PacketType[]{PacketType.Play.Client.TAB_COMPLETE}) {
			@SuppressWarnings("rawtypes")
			public void onPacketReceiving(PacketEvent event) {
				if ((event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE)
						&& (!event.getPlayer().hasPermission("hideandcustomplugins.bypass"))
						&& (((String) event.getPacket().getStrings().read(0)).startsWith("/"))
						&& (((String) event.getPacket().getStrings().read(0)).split(" ").length == 1)) {

					event.setCancelled(true);

					List<?> list = new ArrayList();
					List<?> extra = new ArrayList();

					String[] tabList = new String[list.size() + extra.size()];

					for (int index = 0; index < list.size(); index++) {
						tabList[index] = ((String) list.get(index));
					}

					for (int index = 0; index < extra.size(); index++) {
						tabList[(index + list.size())] = ('/' + (String) extra.get(index));
					}
					PacketContainer tabComplete = manager.createPacket(PacketType.Play.Server.TAB_COMPLETE);
					tabComplete.getStringArrays().write(0, tabList);

					try {
						manager.sendServerPacket(event.getPlayer(), tabComplete);
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}

		});

		getLogger().info("[" + name + "] Plugin has been activated successfully.");
	}
	
	@Override
	public void onDisable() {
		config.saveNow();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		boolean plugins = event.getMessage().toLowerCase().startsWith("/plugins");
		boolean pl = event.getMessage().toLowerCase().startsWith("/pl") && !event.getMessage().toLowerCase().startsWith("/plotme") && !event.getMessage().toLowerCase().startsWith("/plot") && !event.getMessage().toLowerCase().startsWith("/plotgenversion") && !event.getMessage().toLowerCase().startsWith("/pluginmanager") && !event.getMessage().toLowerCase().startsWith("/plugman") && !event.getMessage().toLowerCase().startsWith("/plane") && !event.getMessage().toLowerCase().startsWith("/planeshop") && !event.getMessage().toLowerCase().startsWith("/player") && !event.getMessage().toLowerCase().startsWith("/playtime");
		boolean gc = event.getMessage().equalsIgnoreCase("/gc");
		boolean icanhasbukkit = event.getMessage().toLowerCase().startsWith("/icanhasbukkit");
		boolean unknown = event.getMessage().toLowerCase().startsWith("/?");
		boolean version = event.getMessage().toLowerCase().startsWith("/version");
		boolean ver = event.getMessage().toLowerCase().startsWith("/ver");
		boolean bukkitplugin = event.getMessage().toLowerCase().startsWith("/bukkit:plugins");
		boolean bukkitpl = event.getMessage().toLowerCase().startsWith("/bukkit:pl");
		boolean bukkitunknown = event.getMessage().toLowerCase().startsWith("/bukkit:?");
		boolean about = event.getMessage().toLowerCase().startsWith("/about");
		boolean a = event.getMessage().equalsIgnoreCase("/a");
		boolean bukkitabout = event.getMessage().toLowerCase().startsWith("/bukkit:about");
		boolean bukkita = event.getMessage().toLowerCase().startsWith("/bukkit:a");
		boolean bukkitversion = event.getMessage().toLowerCase().startsWith("/bukkit:version");
		boolean bukkitver = event.getMessage().toLowerCase().startsWith("/bukkit:ver");
		boolean bukkithelp = event.getMessage().toLowerCase().startsWith("/bukkit:help");
		boolean help = event.getMessage().equalsIgnoreCase("/help");

		Player player = event.getPlayer();
		String command = event.getMessage();

		if (!player.hasPermission("hideandcustomplugins.bypass") && config.isBlacklisted(command)) {
			player.sendMessage(config.msg_error.replaceAll("&", "§"));
			event.setCancelled(true);
		}
		
		if(config.block_builtin && !player.hasPermission("hideandcustomplugins.bypass")) {
			if ((plugins) || (pl) || (bukkitunknown) || (unknown) || (bukkitplugin) || (bukkitpl) || (version) || (ver) || (gc) || (icanhasbukkit) || (a) || (about) || (bukkitversion) || (bukkitver) || (bukkitabout) || (bukkita) || (bukkithelp)) {
				event.setCancelled(true);
			}
		} else {

			if ((plugins) || (pl) || (bukkitunknown) || (unknown) || (bukkitplugin) || (bukkitpl)) {
				if (!player.hasPermission("hideandcustomplugins.bypass")) {
					event.setCancelled(true);
					String defaultMessage = "§a";
					for (String plugin : config.plugins) {
						defaultMessage = defaultMessage + plugin + ", ";
					}
					defaultMessage = defaultMessage.substring(0, defaultMessage.lastIndexOf(", "));
					player.sendMessage(ChatColor.WHITE + "Plugins (" + config.plugins.size() + "): " + ChatColor.GREEN + defaultMessage.replaceAll(", ", new StringBuilder().append(ChatColor.WHITE).append(", ").append(ChatColor.GREEN).toString()));
				}
			}

			if ((version) || (ver) || (gc) || (icanhasbukkit) || (a) || (about) || (bukkitversion) || (bukkitver) || (bukkitabout) || (bukkita) || (bukkithelp)) {
				if (!player.hasPermission("hideandcustomplugins.bypass")) {
					Player p = event.getPlayer();
					event.setCancelled(true);
					p.sendMessage(config.msg_error.replaceAll("&", "§"));
				}
			}

		}
		if (config.pot_enabled) {
			if ((plugins) || (pl) || (bukkitunknown) || (unknown) || (bukkitplugin) || (bukkitpl) || (version) || (ver) || (gc) || (icanhasbukkit) || (a) || (about) || (bukkitversion) || (bukkitver) || (bukkitabout) || (bukkita) || (bukkithelp)) {
				if (!player.hasPermission("hideandcustomplugins.bypass")) {
					event.setCancelled(true);
					if(config.pot_type != null) {
						player.addPotionEffect(new PotionEffect(config.pot_type, config.pot_time * 20, 1));
					}
					if(config.pot_sound != null) {
						player.playSound(player.getLocation(), config.pot_sound, 2.0F, 1.0F);
					}
				}
			}
		}
		if (config.help_disabled) {
			if (help) {
				if (!player.hasPermission("hideandcustomplugins.bypass")) {
					Player p = event.getPlayer();
					event.setCancelled(true);
					p.sendMessage(config.msg_error.replaceAll("&", "§"));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (config.update_notify && player.hasPermission("hideandcustomplugins.bypass")) {
			updater.search(player);
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("hcp")) {
				if (args.length == 0) {
					if (sender.hasPermission("hideandcustomplugins.info")) {
						sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
						sender.sendMessage("§9/hcp reload - Reloads the config.yml.\n");
						sender.sendMessage("§9/hcp add <cmd> - Add a command to the blacklist.\n");
						sender.sendMessage("§9/hcp remove <cmd> - Remove a command from the blacklist.\n");
						sender.sendMessage("§9/hcp blacklist - Shows a list with the blocked commands.\n");
						sender.sendMessage("§aHCP protects the server against pluginthieves");
						sender.sendMessage("§5Version: " + ChatColor.DARK_PURPLE + version);
						sender.sendMessage("§5Created by: " + ChatColor.DARK_PURPLE + "LauseggerDevelopment");
						sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
					} else {
						sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.info");
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("reload")) {
					if (sender.hasPermission("hideandcustomplugins.reload")) {
						if (args.length == 1) {
							config.reload();
							sender.sendMessage(ChatColor.GREEN + "Reloaded the config.yml of " + getDescription().getName() + " v" + getDescription().getVersion());
						} else {
							sender.sendMessage("§cFalse or to many arguments!\n§a/hcp - Information about the plugin");
						}
					} else {
						sender.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.reload");
					}
					return true;
				}

				if (args[0].equalsIgnoreCase("add")) {
					if (sender.hasPermission("hideandcustomplugins.add")) {
						if (args.length >= 2) {
							if(config.addToBlacklist(args, 1)) {
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
					return true;
				}

				if (args[0].equalsIgnoreCase("remove")) {
					if (sender.hasPermission("hideandcustomplugins.remove")) {
						if (args.length >= 2) {
							if(config.removeFromBlacklist(args, 1)) {
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
					return true;
				}

				if (args[0].equalsIgnoreCase("blacklist")) {
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
					return true;
				}
			} else {
				if (args.length == 0) {
					sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
					sender.sendMessage("§9/hcp reload - Reloads the config.yml.\n");
					sender.sendMessage("§9/hcp add <cmd> - Add a command to the blacklist.\n");
					sender.sendMessage("§9/hcp remove <cmd> - Remove a command from the blacklist.\n");
					sender.sendMessage("§9/hcp blacklist - Shows a list with the blocked commands.\n");
					sender.sendMessage("§aHCP protects the server against pluginthieves");
					sender.sendMessage("§5Version: " + ChatColor.DARK_PURPLE + version);
					sender.sendMessage("§5Created by: " + ChatColor.DARK_PURPLE + "LauseggerDevelopment");
					sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
					return true;
				}

				if (args[0].equalsIgnoreCase("reload")) {
					if (args.length == 1) {
						reloadConfig();
						sender.sendMessage(ChatColor.GREEN + "Reloaded the config.yml of " + getDescription().getName() + " v" + getDescription().getVersion());
						return true;
					}
					if (args.length > 1) {
						sender.sendMessage("§cFalse or to many arguments!\n§a/hcp - Information about the plugin");
						return true;
					}
				}

				if ((args.length > 0) && !(args[0].equalsIgnoreCase("reload"))) {
					sender.sendMessage("§cFalse or to many arguments!\n§a/hcp - Information about the plugin");
					return true;
				}

			}
		return false;
	}
}
