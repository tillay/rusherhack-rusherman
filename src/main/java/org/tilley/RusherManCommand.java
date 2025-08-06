package org.tilley;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.core.command.annotations.CommandExecutor;
import org.rusherhack.core.command.argument.StringCapture;
import java.io.File;
import java.util.Objects;

import static org.tilley.PluginUtils.PluginMetadata.parsePluginJarMetadata;

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

    @CommandExecutor(subCommand = "list")
    private String listAll() throws Exception {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled")) {
                fileString.append("\n").append(parsePluginJarMetadata(file).getName());
            }
        }
        return "All Plugins: "+fileString;
    }

    @CommandExecutor(subCommand = "list enabled")
    private String listEnabled() throws Exception {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar") && !isFailed(file.getName())) {
                fileString.append("\n").append(parsePluginJarMetadata(file).getName());
            }
        }
        return "Plugins that are enabled: "+fileString;
    }

    @CommandExecutor(subCommand = "list disabled")
    private String listDisabled() throws Exception {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar.disabled")) {
                fileString.append("\n").append(parsePluginJarMetadata(file).getName());
            }
        }
        return "Plugins that were disabled manually: "+fileString;
    }

    @CommandExecutor(subCommand = "list failed")
    private String listFailed() throws Exception {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (isFailed(file.getName())) {
                fileString.append("\n").append(parsePluginJarMetadata(file).getName());
            }
        }
        return "Plugins that failed to start: "+fileString;
    }
    @CommandExecutor(subCommand = "jar")
    @CommandExecutor.Argument("plugin name")
    private String fileFromName(StringCapture pluginName) throws Exception {
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled")) {
                if (Objects.equals(parsePluginJarMetadata(file).getName(), pluginName.string())) {
                    return file.getName();
                }
            }
        }
        return "No plugin found with name "+pluginName.string();
    }

    @CommandExecutor(subCommand = "status")
    @CommandExecutor.Argument("plugin name")
    private String pluginStatus(StringCapture pluginName) throws Exception {
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar")) {
                return ("Plugin "+pluginName.string()+" is enabled");
            }
            else if (file.getName().endsWith(".jar.disabled")) {
                return ("Plugin "+pluginName.string()+" is disabled");
            }

        }
        return "No plugin found with name "+pluginName.string();
    }
    @CommandExecutor(subCommand = "enable")
    @CommandExecutor.Argument("plugin file name")
    private String enablePlugin(StringCapture pluginName) throws Exception {
        File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
        if (pluginFile.isFile()) {
            if (pluginFile.getName().endsWith(".jar.disabled")) {
                pluginFile.renameTo(new File(pluginFile.getParent(), pluginFile.getName().replaceFirst("\\.disabled$", "")));
                return "Successfully enabled plugin " + pluginName.string();
            } else if (pluginFile.getName().endsWith(".jar")) {
                return "Plugin " +  pluginName.string() + " is already enabled";
            }
        }
        return "File not found: " +  pluginName.string();
    }

    @CommandExecutor(subCommand = "disable")
    @CommandExecutor.Argument("plugin file name")
    private String disablePlugin(StringCapture pluginName) throws Exception {
        File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
        if (pluginFile.isFile()) {
            if (parsePluginJarMetadata(pluginFile).getMixinConfigs() != null) {
                return "Plugin " + pluginName.string() + " is a core plugin and cannot be disabled by RusherMan";
            }
            if (pluginFile.getName().endsWith(".jar")) {
                pluginFile.renameTo(new File(pluginFile.getParent(), pluginFile.getName().replaceFirst(".jar", ".jar.disabled")));
                return "Successfully disabled plugin " + pluginName.string();
            } else if (pluginFile.getName().endsWith(".jar.disabled")) {
                return "Plugin " +  pluginName.string() + " is already disabled";
            }
        }
        return "File not found: " +  pluginName.string();
    }
}