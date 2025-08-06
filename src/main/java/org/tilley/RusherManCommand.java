package org.tilley;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;

import java.io.File;

public class RusherManCommand extends Command {


    public RusherManCommand() {
        super("rusherman", "Rusherhack Package Manager");
    }

    private String getPluginDir() {
        return mc.gameDirectory.toPath() + "/rusherhack/plugins";
    }

    private File[] getFiles() {
        File dir = new File(getPluginDir());
        return dir.listFiles(File::isFile);
    }

    private boolean isFailed(String pluginName) {
        return false; // TODO MAKE THIS WORK WHEN JOHN FIXES HIS SRC
    }

    @CommandExecutor(subCommand = "list all")
    private String listAll() {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled")) {
                fileString.append(file.getName()).append("\n");
            }
        }
        return fileString.toString();
    }

    @CommandExecutor(subCommand = "list enabled")
    private String listEnabled() {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar") && !isFailed(file.getName())) {
                fileString.append(file.getName()).append("\n");
            }
        }
        return fileString.toString();
    }

    @CommandExecutor(subCommand = "list disabled")
    private String listDisabled() {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar.disabled")) {
                fileString.append(file.getName()).append("\n");
            }
        }
        return fileString.toString();
    }

    @CommandExecutor(subCommand = "list failed")
    private String listFailed() {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (isFailed(file.getName())) {
                fileString.append(file.getName()).append("\n");
            }
        }
        return fileString.toString();
    }

    @CommandExecutor(subCommand = "enable")
    @CommandExecutor.Argument("plugin file name")
    private String enablePlugin(String pluginName) {
        File pluginFile = new File(getPluginDir(), pluginName);
        if (pluginFile.isFile()) {
            if (pluginFile.getName().endsWith(".jar.disabled")) {
                pluginFile.renameTo(new File(pluginFile.getParent(), pluginFile.getName().replaceFirst("\\.disabled$", "")));
                return "Successfully enabled plugin " + pluginName;
            } else if (pluginFile.getName().endsWith(".jar")) {
                return "Plugin " + pluginName + " is already enabled";
            } else {
                return "File " + pluginName + " is not a plugin!";
            }
        }
        return "File not found: " + pluginName;
    }
}