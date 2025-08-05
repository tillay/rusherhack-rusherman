package org.tilley;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;

import java.io.File;

import static org.tilley.PluginUtils.PluginMetadata.parsePluginJarMetadata;

public class RusherManCommand extends Command {


    public RusherManCommand() {
        super("rusherman", "Rusherhack Package Manager");
    }

    private String getPluginDir() {
        return mc.gameDirectory.toPath()+"/rusherhack/plugins";
    }

    private File[] getJarFiles() {
        File dir = new File(getPluginDir());
        return dir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
    }

    @CommandExecutor(subCommand = "listInstalled")
    private String listInstalled() {
        StringBuilder fileString = new StringBuilder();
        try {
            for (int i = 0; i < getJarFiles().length; i++) {
                fileString.append(parsePluginJarMetadata(getJarFiles()[i]));
            }
            return fileString.toString();
        } catch (Exception ignored) {
            return "wtf";
        }
    }

}