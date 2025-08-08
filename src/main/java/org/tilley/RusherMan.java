package org.tilley;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.rusherhack.client.api.Globals.mc;

public class RusherMan extends Plugin {
	
	@Override
	public void onLoad() {
		
		this.getLogger().info("Loaded rusherman plugin!");
		try {
			Path path = mc.gameDirectory.toPath().resolve("rusherhack/cache/plugins-and-themes.json");
			Files.createDirectories(path.getParent());

			try (InputStream in = new URL("https://raw.githubusercontent.com/RusherDevelopment/rusherhack-plugins/refs/heads/main/generated/json/plugins-and-themes.json").openStream()) {
				Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			}
			this.getLogger().info("Synced plugin json file!");
		} catch (Exception e) {
			this.getLogger().error("Failed to download plugin json file!");
		}

		final RusherManCommand rusherManCommand = new RusherManCommand();
		RusherHackAPI.getCommandManager().registerFeature(rusherManCommand);

		final confirm confirm = new confirm();
		RusherHackAPI.getCommandManager().registerFeature(confirm);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("rusherman plugin unloaded!");
	}
	
}