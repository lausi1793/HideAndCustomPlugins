package at.mcnetwork.lausi;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.Plugin;

public class TabCompleteListener {

	public static void installHook(HideAndCustomPlugins plugin) {
		final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		final PacketListener listener = new PacketAdapter(plugin, new PacketType[]{PacketType.Play.Client.TAB_COMPLETE}) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				if ((event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE)
						&& (!event.getPlayer().hasPermission("hideandcustomplugins.bypass"))
						&& (((String) event.getPacket().getStrings().read(0)).startsWith("/"))
						&& (((String) event.getPacket().getStrings().read(0)).split(" ").length == 1)) {

					event.setCancelled(true);

					Config config = ((HideAndCustomPlugins) this.plugin).config;
					
					String[] tabList;
					if(config.plugin_blacklist.isEmpty()) {
						tabList = config.whitelist.toArray(new String[0]);
					} else {
						List<String> canUse = new LinkedList<String>();
						canUse.addAll(config.whitelist);
						boolean useAll = event.getPlayer().hasPermission("hideandcustomplugins.plugin.*");
						for(Map.Entry<String, List<String>> e : config.plugin_blacklist.entrySet()) {
							if(e.getValue() != null && !e.getValue().isEmpty() && 
									(useAll || event.getPlayer().hasPermission("hideandcustomplugins.plugin." + e.getKey()))) {
								canUse.addAll(e.getValue());
							}
						}
						tabList = canUse.toArray(new String[canUse.size()]);
					}

					for (int index = 0; index < tabList.length; ++index) {
						tabList[index] = '/' + tabList[index];
					}

					PacketContainer tabComplete = manager.createPacket(PacketType.Play.Server.TAB_COMPLETE);
					tabComplete.getStringArrays().write(0, tabList);

					try {
						manager.sendServerPacket(event.getPlayer(), tabComplete);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		manager.addPacketListener(listener);
	}

}
