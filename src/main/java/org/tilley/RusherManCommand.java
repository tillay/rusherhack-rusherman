package org.tilley;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;
import org.rusherhack.core.command.argument.StringCapture;
import org.rusherhack.client.api.utils.ChatUtils;

import java.io.*;
import java.net.URL;

import java.util.Objects;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import static org.tilley.ComponentUtils.*;

import static org.tilley.PluginMetadata.parsePluginJarMetadata;

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

    @CommandExecutor(subCommand = "list")
    private String listAll() throws Exception {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled")) {
                fileString.append("\n").append(parsePluginJarMetadata(file).getName());
            }
        }
        return "All Plugins: " + fileString;
    }

    @CommandExecutor(subCommand = "list enabled")
    private String listEnabled() throws Exception {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar")) {
                fileString.append("\n").append(parsePluginJarMetadata(file).getName());
            }
        }
        return "Plugins that are enabled: " + fileString;
    }

    @CommandExecutor(subCommand = "list disabled")
    private String listDisabled() throws Exception {
        StringBuilder fileString = new StringBuilder();
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar.disabled")) {
                fileString.append("\n").append(parsePluginJarMetadata(file).getName());
            }
        }
        return "Plugins that were disabled manually: " + fileString;
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
        return "No plugin found with name " + pluginName.string();
    }

    @CommandExecutor(subCommand = "status")
    @CommandExecutor.Argument("plugin name")
    private String pluginStatus(StringCapture pluginName) throws Exception {
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar")) {
                return ("Plugin " + pluginName.string() + " is enabled");
            } else if (file.getName().endsWith(".jar.disabled")) {
                return ("Plugin " + pluginName.string() + " is disabled");
            }

        }
        return "No installed plugin found with name " + pluginName.string();
    }

    @CommandExecutor(subCommand = "enable")
    @CommandExecutor.Argument("plugin name (case sensitive)")
    private String enablePlugin(StringCapture pluginName) throws Exception {
        File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
        if (pluginFile.isFile()) {
            if (pluginFile.getName().endsWith(".jar.disabled")) {
                pluginFile.renameTo(new File(pluginFile.getParent(), pluginFile.getName().replaceFirst("\\.disabled$", "")));
                return "Successfully enabled plugin " + pluginName.string();
            } else if (pluginFile.getName().endsWith(".jar")) {
                return "Plugin " + pluginName.string() + " is already enabled";
            }
        }
        return "File not found: " + pluginName.string();
    }

    @CommandExecutor(subCommand = "disable")
    @CommandExecutor.Argument("plugin name (case sensitive)")
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
                return "Plugin " + pluginName.string() + " is already disabled";
            }
        }
        return "File not found: " + pluginName.string();
    }


    // BELOW IS FUNCTIONS THAT WORK WITH THE REMOTE LIST PROVIDED BY GARLIC
    private JsonArray getRemotePluginsJson() {
        Path path = mc.gameDirectory.toPath().resolve("rusherhack/cache/plugins-and-themes.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return root.getAsJsonArray("plugins");
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    @CommandExecutor(subCommand = "list all")
    private Component listAllRemote() {
        MutableComponent names = (MutableComponent) makeComponent("All plugins in the repo: \n", 0xffffff);
        for (JsonElement plugin : getRemotePluginsJson()) {
            JsonObject obj = plugin.getAsJsonObject();
            String range = obj.get("mc_versions").getAsString();
            if (isVersionCompatible(range))
                names.append(linkify(obj.get("name").getAsString()+" ", "https://github.com/"+obj.get("repo").getAsString(), 0x00ff00));
            else {
                names.append(linkify(obj.get("name").getAsString()+" ", "https://github.com/"+obj.get("repo").getAsString(), 0xff0000));
            }
        }
        return names;
    }

    @CommandExecutor(subCommand = "list comp")
    private Component listCompRemote() {
        MutableComponent names = (MutableComponent) makeComponent("Available plugins to be downloaded: \n", 0xffffff);
        for (JsonElement plugin : getRemotePluginsJson()) {
            JsonObject obj = plugin.getAsJsonObject();
            String range = obj.get("mc_versions").getAsString();
            if (isVersionCompatible(range)) {
                names.append(linkify(obj.get("name").getAsString()+" ", "https://github.com/"+obj.get("repo").getAsString(), 0x00ff00));
            }
        }
        return names;
    }

    private boolean isVersionCompatible(String range) {
        String supported = net.minecraft.SharedConstants.getCurrentVersion().getName();
        if (range.contains("-")) {
            String[] parts = range.split("-");
            return compareVersion(supported, parts[0]) >= 0 && compareVersion(supported, parts[1]) <= 0;
        }
        return supported.equals(range);
    }

    // Chatgpt made this im sorry but this was hurting my brain
    private int compareVersion(String a, String b) {
        String[] sa = a.split("\\.");
        String[] sb = b.split("\\.");
        int len = Math.max(sa.length, sb.length);
        for (int i = 0; i < len; i++) {
            int va = i < sa.length ? Integer.parseInt(sa[i]) : 0;
            int vb = i < sb.length ? Integer.parseInt(sb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    @CommandExecutor(subCommand = "getinfo")
    @CommandExecutor.Argument("plugin name")
    private String getPluginInfo(StringCapture pluginName) throws Exception {
        for (JsonElement plugin : getRemotePluginsJson()) {
            JsonObject obj = plugin.getAsJsonObject();
            if (!obj.get("name").getAsString().equalsIgnoreCase(pluginName.string())) continue;

            return "Information about plugin " + obj.get("name").getAsString() + ":\n"
                    + "Author: " + obj.getAsJsonObject("creator").get("name").getAsString() + "\n"
                    + "Description: " + obj.get("description").getAsString() + "\n"
                    + "Plugin Repo: https://github.com/" + obj.get("repo").getAsString() + "\n"
                    + "Supported versions: " + obj.get("mc_versions").getAsString();
        }
        return "Plugin not found";
    }

    @CommandExecutor(subCommand = "downloads")
    @CommandExecutor.Argument("plugin name")
    private Component listDownloads(StringCapture pluginName) throws Exception {
        List<String> urls = getReleaseLink(pluginName.string());
        if (urls == null) return makeComponent("No plugin with that name found", 0xff0000);
        MutableComponent urlList = (MutableComponent) makeComponent("Release Download URLs:\n", 0xffffff);
        for (String url : urls) urlList.append(linkify(url+"\n", url, 0x00FFFF));
        return urlList;
    }


    private List<String> getReleaseLink(String pluginName) throws Exception {
        for (JsonElement plugin : getRemotePluginsJson()) {
            JsonObject obj = plugin.getAsJsonObject();
            if (!obj.get("name").getAsString().equalsIgnoreCase(pluginName)) continue;
            String api_link = "https://api.github.com/repos/" + obj.get("repo").getAsString() + "/releases/latest";
            URL url = new URL(api_link);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            JsonArray assets = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("assets");
            List<String> urls = new ArrayList<>();
            for (JsonElement e : assets)
                urls.add(e.getAsJsonObject().get("browser_download_url").getAsString());
            return urls;
        }
        return null;
    }
    public static List<String> downloadLinks = new ArrayList<>();

    @CommandExecutor(subCommand = "install")
    @CommandExecutor.Argument("plugin name or download link")
    private Component installPlugin(StringCapture userInput) throws Exception {
        if (userInput.string().startsWith("https://")&&userInput.string().endsWith(".jar")) {
            ChatUtils.print("Detected link for manual download...");
            downloadLinks.add(userInput.string());
            disclaimer();
            return makeComponent("use *confirm to continue download", 0xffff00);
        } else {
                List<String>possibleLinks =  getReleaseLink(userInput.string());
                if (possibleLinks != null) {
                    if (possibleLinks.size() == 1) {
                        ChatUtils.print(((MutableComponent) makeComponent("Found download candidate! Downloading from ", 0x00ff00)).append(linkify(possibleLinks.getFirst(), possibleLinks.getFirst(), 0x00FFFF)));
                        downloadLinks.add(possibleLinks.getFirst());
                        disclaimer();
                        return makeComponent("use *confirm to continue download", 0xffff00);
                    } else {
                        ChatUtils.print("Detected Multiple links");
                        int i = 1;
                        for (String link : possibleLinks) {
                            downloadLinks.add(link);
                            ChatUtils.print(((MutableComponent) makeComponent(i++ + ". ", 0xffffff)).append(linkify(link, link, 0x00FFFF)));
                        }
                        disclaimer();
                        return makeComponent("use *confirm <number> to download the plugin from said link", 0xffff00);
                    }
                }
                return makeComponent("Plugin not found! Please input a file link or plugin name!\nYou can find plugin names by using *rusherman list remote compatible", 0xFF0000);
        }
    }


}

