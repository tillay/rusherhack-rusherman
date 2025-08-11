package org.tilley.rusherman;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.rusherhack.core.command.argument.StringCapture;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.rusherhack.client.api.Globals.mc;
import static org.tilley.rusherman.ComponentUtils.linkify;
import static org.tilley.rusherman.ComponentUtils.makeComponent;
import static org.tilley.rusherman.PluginMetadata.parsePluginJarMetadata;

public class WindowUtils {
    
        public String getPluginDir() {
            return mc.gameDirectory.toPath() + "/rusherhack/plugins";
        }

        public File[] getFiles() {
            File dir = new File(getPluginDir());
            return dir.listFiles(File::isFile);
        }

        public List<Component> listInstalled() throws Exception {
            List<Component> outputComponents = new ArrayList<>();
            
            for (File file : getFiles()) {
                if (file.getName().endsWith(".jar")) {
                    outputComponents.add(makeComponent(parsePluginJarMetadata(file).getName(), 0x00ff00));
                }
                else if (file.getName().endsWith(".jar.disabled")) {
                    outputComponents.add(makeComponent(parsePluginJarMetadata(file).getName(), 0xff0000));
                }
            }
            return outputComponents;
        }
        
        
        
        public String fileFromName(StringCapture pluginName) throws Exception {
            for (File file : getFiles()) {
                if (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled")) {
                    if (Objects.equals(parsePluginJarMetadata(file).getName(), pluginName.string())) {
                        return file.getName();
                    }
                }
            }
            return "No plugin found with name " + pluginName.string();
        }


        public String pluginStatus(StringCapture pluginName) throws Exception {
            for (File file : getFiles()) {
                if (file.getName().endsWith(".jar")) {
                    return ("Plugin " + pluginName.string() + " is enabled");
                } else if (file.getName().endsWith(".jar.disabled")) {
                    return ("Plugin " + pluginName.string() + " is disabled");
                }

            }
            return "No installed plugin found with name " + pluginName.string();
        }
        
        public Component removePlugin(StringCapture pluginName) throws Exception {
            File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
            if (pluginFile.isFile()) {
                if (pluginFile.delete()) return makeComponent("Removed: " + pluginName.string(), 0xff0000);
                return makeComponent("Failed to remove: " + pluginName.string(), 0xff0000);
            }
            return makeComponent("Plugin not found: " + pluginName.string(), 0xff0000);
        }
        
        public String disablePlugin(StringCapture pluginName) throws Exception {
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


        public Component enablePlugin(StringCapture pluginName) throws Exception {
            File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
            if (pluginFile.isFile()) {
                if (pluginFile.getName().endsWith(".jar.disabled")) {
                    pluginFile.renameTo(new File(pluginFile.getParent(), pluginFile.getName().replaceFirst("\\.disabled$", "")));
                    return makeComponent("Successfully enabled plugin " + pluginName.string(), 0xffffff);
                } else if (pluginFile.getName().endsWith(".jar")) {
                    return makeComponent("Plugin " + pluginName.string() + " is already enabled", 0xffffff);
                }
            }
            return makeComponent("File not found: " + pluginName.string(), 0xffffff);
        }

        // BELOW IS FUNCTIONS THAT WORK WITH THE REMOTE LIST PROVIDED BY GARLIC
        public JsonArray getRemotePluginsJson() {
            Path path = mc.gameDirectory.toPath().resolve("rusherhack/cache/plugins-and-themes.json");
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return root.getAsJsonArray("plugins");
            } catch (Exception e) {
                return new JsonArray();
            }
        }

        public Component listAllRemote() {
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

        public Component listCompRemote() {
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

        public boolean isVersionCompatible(String range) {
            String supported = SharedConstants.getCurrentVersion().getName();
            if (range.contains("-")) {
                String[] parts = range.split("-");
                return compareVersion(supported, parts[0]) >= 0 && compareVersion(supported, parts[1]) <= 0;
            }
            return supported.equals(range);
        }

        // Chatgpt made this im sorry but this was hurting my brain
        public int compareVersion(String a, String b) {
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


        public String getPluginInfo(StringCapture pluginName) throws Exception {
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

        public Component listDownloads(StringCapture pluginName) throws Exception {
            List<String> urls = getReleaseLink(pluginName.string());
            if (urls == null) return makeComponent("No plugin with that name found", 0xff0000);
            MutableComponent urlList = (MutableComponent) makeComponent("Release Download URLs:\n", 0xffffff);
            for (String url : urls) urlList.append(linkify(url+"\n", url, 0x00FFFF));
            return urlList;
        }

        public List<String> getReleaseLink(String pluginName) throws Exception {
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


    }
    
