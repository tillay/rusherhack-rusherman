package org.tilley;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class RusherMan extends Plugin {
	
	@Override
	public void onLoad() {
		
		this.getLogger().info("Loaded rusherman plugin!");


		final RusherManCommand rusherManCommand = new RusherManCommand();
		RusherHackAPI.getCommandManager().registerFeature(rusherManCommand);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("rusherman plugin unloaded!");
	}
	
}