package at.mcnetwork.lausi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

/**
 * 
 * @author Michael Lausegger | LauseggerDevelopment
 * @version 1.5.1
 * @since May 16, 2014
 *
 */
public class HideAndCustomPlugins extends JavaPlugin implements Listener {
	
	
	ProtocolManager protocolManager;
	public ArrayList<String> plugins = new ArrayList<String>();
	public ArrayList<String> blacklist = new ArrayList<String>();
	String version;
	String name;
	
	public void onEnable() {
		version = getDescription().getVersion();
		name = getDescription().getName();
		final List<String> hiddenCommands = new ArrayList<String>();
		hiddenCommands.add("all");
		
		try {
			Metrics metrics = new Metrics(this); metrics.start();
			Logger.getLogger("Minecraft").info("[" + name + "] Version: " + version + " Metrics started: http://mcstats.org/plugin/HideAndCustomPlugins");
			
			} catch (IOException e) {
			System.out.println("Error Submitting stats!");
			}
		loadConfig();
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		
		if(getConfig().getBoolean("update-notification")){
			try {
				new Updater(this, 80016, "http://dev.bukkit.org/bukkit-plugins/hideandcustomplugins/", "SearchForUpdates").search();
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
		}
		
		final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
	    manager.addPacketListener(new PacketAdapter(this, new PacketType[] { PacketType.Play.Client.TAB_COMPLETE })
	    {
		@SuppressWarnings("rawtypes")
		public void onPacketReceiving(PacketEvent event) {
	        if ((event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) 
	        		&& (!event.getPlayer().hasPermission("hideandcustomplugins.bypass")) 
	        		&& (((String)event.getPacket().getStrings().read(0)).startsWith("/"))
	        		) {
	        	
	          event.setCancelled(true);

	          List<?> list = new ArrayList();
	          List<?> extra = new ArrayList();

	          String[] tabList = new String[list.size() + extra.size()];

	          for (int index = 0; index < list.size(); index++) {
	            tabList[index] = ((String)list.get(index));
	          }

	          for (int index = 0; index < extra.size(); index++) {
	            tabList[(index + list.size())] = ('/' + (String)extra.get(index));
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
    
	    for (String s : getConfig().getString("plugins").split(", ")) {
		      this.plugins.add(s);
		    }
		Logger.getLogger("Minecraft").info("[" + name + "] Version: " + version + " Plugin has been activated successfully.");
	}
	
	
	public void onDisable() {
		saveDefaultConfig();
		Logger.getLogger("Minecraft").info("[" + name + "] Version: " + version + " Plugin was disabled successfully.");
	}
	
	private void loadConfig() {
		FileConfiguration cfg = this.getConfig();
		cfg.options().copyDefaults(true);
		this.saveDefaultConfig();
		Logger.getLogger("Minecraft").info("[" + name + "] Version: " + version+ " Successfully reloaded config.yml");
	}
	
	
	@EventHandler(priority = EventPriority.HIGHEST)
	  public void onCommand(PlayerCommandPreprocessEvent event) {
	    boolean plugins = event.getMessage().startsWith("/plugins");
	    boolean pl = event.getMessage().equalsIgnoreCase("/pl");
	    boolean pl2 = event.getMessage().startsWith("/pl");
	    boolean gc = event.getMessage().equalsIgnoreCase("/gc");
	    boolean icanhasbukkit = event.getMessage().startsWith("/icanhasbukkit");
	    boolean unknown = event.getMessage().startsWith("/?");
	    boolean version = event.getMessage().startsWith("/version");
	    boolean ver = event.getMessage().startsWith("/ver");
	    boolean bukkitplugin = event.getMessage().startsWith("/bukkit:plugins");
	    boolean bukkitpl = event.getMessage().startsWith("/bukkit:pl");
	    boolean bukkitunknown = event.getMessage().startsWith("/bukkit:?");
	    boolean about = event.getMessage().startsWith("/about");
	    boolean a = event.getMessage().equalsIgnoreCase("/a");
	    boolean bukkitabout = event.getMessage().startsWith("/bukkit:about");
	    boolean bukkita = event.getMessage().startsWith("/bukkit:a");
	    boolean bukkitversion = event.getMessage().startsWith("/bukkit:version");
	    boolean bukkitver = event.getMessage().startsWith("/bukkit:ver");
	    boolean bukkithelp = event.getMessage().startsWith("/bukkit:help");
	    boolean help = event.getMessage().startsWith("/help");
	    
	    Player player = event.getPlayer();
	    String command = event.getMessage();
	    
	    if(!player.hasPermission("hideandcustomplugins.bypass")){
	    	for(int i = 0; i < getConfig().getList("blocked-cmds").size(); i++){
	    		String playercommand = (String) getConfig().getList("blocked-cmds").get(i);
	    		if(command.toUpperCase().contains("/" + playercommand.toUpperCase())){
	    			Player p = event.getPlayer();
	    			p.sendMessage(getConfig().getString("error-message").replaceAll("&", "§"));
	    			event.setCancelled(true);
	    		}
	    	}
	    }
	    
	    if(getConfig().getBoolean("disable-messages")){ 
	    	if ((plugins) || (pl) || (pl2) || (bukkitunknown) ||  (unknown) ||  (bukkitplugin) ||  (bukkitpl) || (version) || (ver) ||  (gc) ||  (icanhasbukkit) ||  (a) ||  (about) ||  (bukkitversion) ||  (bukkitver)||  (bukkitabout)  ||  (bukkita) ||  (bukkithelp)) {
	 	    	if(!player.hasPermission("hideandcustomplugins.bypass")){
	 	    		event.setCancelled(true);}
	 	    }
	    
	    }else{
	    	
	    	if ((plugins) || (pl) || (pl2) || (bukkitunknown) ||  (unknown) ||  (bukkitplugin) ||  (bukkitpl)) {
		    	if(!player.hasPermission("hideandcustomplugins.bypass")){
		    		event.setCancelled(true);
		    		String defaultMessage = "§a";
		    		for (String plugin : this.plugins) {
		    			defaultMessage = defaultMessage + plugin + ", ";
		    		}
		    		defaultMessage = defaultMessage.substring(0, defaultMessage.lastIndexOf(", "));
		    		player.sendMessage(ChatColor.WHITE + "Plugins (" + this.plugins.size() + "): " + ChatColor.GREEN + defaultMessage.replaceAll(", ", new StringBuilder().append(ChatColor.WHITE).append(", ").append(ChatColor.GREEN).toString()));
		    	}
		    }
		  
		
		if ((version) || (ver) ||  (gc) ||  (icanhasbukkit) ||  (a) ||  (about) ||  (bukkitversion) ||  (bukkitver)||  (bukkitabout)  ||  (bukkita) ||  (bukkithelp)) {
	    	if(!player.hasPermission("hideandcustomplugins.bypass")){
	    		Player p = event.getPlayer();
	    		event.setCancelled(true);
	    		p.sendMessage(getConfig().getString("error-message").replaceAll("&", "§"));
	    	}
	    }
	    	
	    }
	
	if(getConfig().getBoolean("disable-help-command")){
		if (help) {
			if(!player.hasPermission("hideandcustomplugins.bypass")){
				Player p = event.getPlayer();
				event.setCancelled(true);
				p.sendMessage(getConfig().getString("error-message").replaceAll("&", "§"));
			}
		}
	}
  }
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		if((getConfig().getBoolean("HideAndCustomPlugins.updateNotification")) && (player.hasPermission("hideandcustomplugins.bypass"))){
			try {
				new Updater(this, 80016, "http://dev.bukkit.org/bukkit-plugins/hideandcustomplugins/", "SearchForUpdates").search(player);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){

	Player p = null;
	if ((sender instanceof Player)) {
		p = (Player)sender;
	}
	    
	if (cmd.getName().equalsIgnoreCase("hcp")){
		if (p != null){
			if(args.length == 0){
				if (p.hasPermission("hideandcustomplugins.info")) {
					p.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
					p.sendMessage(ChatColor.GREEN + "Hy " + p.getDisplayName() + ChatColor.GREEN + "!");
					p.sendMessage("§9/hcp reload - Reloads the config.yml.\n");
					p.sendMessage("§aHCP protects the server against pluginthieves");
					p.sendMessage("§5Version: " + ChatColor.DARK_PURPLE + version);
					p.sendMessage("§5Created by: " + ChatColor.DARK_PURPLE + "LauseggerDevelopment");
					p.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
					return true;
				}else{
					p.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.info");
					return true;
				}
			}			
			if(args[0].equalsIgnoreCase("reload")){
				if (p.hasPermission("hideandcustomplugins.reload")){
					reloadConfig();
					p.sendMessage(ChatColor.GREEN + "Reloaded the config.yml of " + getDescription().getName() + " v" + getDescription().getVersion());
					return true;
		        }else{
		        	 p.sendMessage("§cYou dont have the permission\n§c-hideandcustomplugins.reload");
		        	 return true;
		        }
			}
			if((args.length > 0) && !(args[0].equalsIgnoreCase("reload"))){		  
	    		  sender.sendMessage("§cFalse or to many arguments!\n§a/hcp - Information about the plugin");
	    		  return true;
	    	}
		}else{
			if(args.length == 0){
					sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
					sender.sendMessage("§9/hcp reload - Reloads the config.yml.\n");
					sender.sendMessage("§aHCP protects the server against pluginthieves");
					sender.sendMessage("§5Version: " + ChatColor.DARK_PURPLE + version);
					sender.sendMessage("§5Created by: " + ChatColor.DARK_PURPLE + "LauseggerDevelopment");
					sender.sendMessage("§e=========[ HideAndCustomPlugins | Version: " + ChatColor.YELLOW + version + " §e]=========");
					return true;
			}
    	  
			if((args.length > 0) && !(args[0].equalsIgnoreCase("reload"))){		  
	    		  sender.sendMessage("§cFalse or to many arguments!\n§a/hcp - Information about the plugin");
	    		  return true;
	    	}
    	  
		}
    }
	
return false;
	}

	

}
