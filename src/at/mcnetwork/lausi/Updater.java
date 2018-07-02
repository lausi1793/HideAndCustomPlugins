package at.mcnetwork.lausi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updater {

	private String remoteVersionName;
	private int remoteVersion;
	private final int ourVersion;
	private int task;
	private final URL url;
	public final JavaPlugin main;
	public String verwith;
	public String link;
	private String configValue;

	public Updater(JavaPlugin main, int id, String link, String configValue) {
		this.main = main;
		this.ourVersion = Integer.valueOf(main.getDescription().getVersion().replaceAll("\\.", ""));
		this.link = link;
		this.configValue = configValue;
		URL u = null;
		try {
			u = new URL("https://api.curseforge.com/servermods/files?projectIds=" + id);
		} catch (MalformedURLException ex) {
		}
		this.url = u;
	}

	public void search() {
		if (!isEnabled()) {
			return;
		}
		this.task = this.main.getServer().getScheduler().scheduleSyncDelayedTask(this.main, new Runnable() {
			@Override
			public void run() {
				if (!Updater.this.read()) {
					return;
				}
				if (Updater.this.versionCheck(Updater.this.remoteVersionName)) {
					Updater.this.main.getLogger().warning("A new update is available! (" + Updater.this.verwith + ") current: " + Updater.this.main.getDescription().getVersion());
					Updater.this.main.getLogger().warning("You can get it at: " + Updater.this.link);
				}
				Updater.this.main.getServer().getScheduler().cancelTask(Updater.this.task);
			}
		},
				200L);
	}

	public void search(Player player) {
		if (!read()) {
			return;
		}
		if (versionCheck(this.remoteVersionName)) {
			player.sendMessage(ChatColor.GOLD + this.main.getDescription().getName() + ChatColor.GRAY + " A new update is available! \n(" + this.verwith + ") current: " + this.main.getDescription().getVersion());
			player.sendMessage(ChatColor.GOLD + this.main.getDescription().getName() + ChatColor.GRAY + " You can get it at: \n" + this.link);
		}
	}

	private boolean versionCheck(String title) {
		String[] titleParts = title.split(" v");
		this.remoteVersion = Integer.valueOf(titleParts[1].split(" ")[0].replaceAll("\\.", ""));
		this.verwith = titleParts[1].split(" ")[0];
		if (this.ourVersion > this.remoteVersion) {
			return false;
		}
		return (this.ourVersion != this.remoteVersion) && (this.ourVersion <= this.remoteVersion);
	}

	public boolean read() {
		if (url == null) {
			return false;
		}
		try {
			URLConnection conn = this.url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setDoOutput(true);

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String response = reader.readLine();

			JSONArray array = (JSONArray) JSONValue.parse(response);
			if (array.isEmpty()) {
				return false;
			}
			this.remoteVersionName = ((String) ((JSONObject) array.get(array.size() - 1)).get("name"));
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public boolean isEnabled() {
		return url != null && this.main.getConfig().getBoolean(this.configValue, true);
	}
}
