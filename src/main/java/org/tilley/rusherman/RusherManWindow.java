package org.tilley.rusherman;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.window.PopupWindow;
import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.feature.window.Window;
import org.rusherhack.client.api.ui.window.content.ComboContent;
import org.rusherhack.client.api.ui.window.content.ListItemContent;
import org.rusherhack.client.api.ui.window.content.WindowContent;
import org.rusherhack.client.api.ui.window.content.component.ButtonComponent;
import org.rusherhack.client.api.ui.window.content.component.ParagraphComponent;
import org.rusherhack.client.api.ui.window.content.component.TextComponent;
import org.rusherhack.client.api.ui.window.content.component.TextFieldComponent;
import org.rusherhack.client.api.ui.window.context.ContextAction;
import org.rusherhack.client.api.ui.window.view.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.tilley.rusherman.PluginMetadata.parsePluginJarMetadata;


public class RusherManWindow extends ResizeableWindow {
    public static List<String> guiLinks = new ArrayList<>();

    public static RusherManWindow INSTANCE;
    private final TabbedView tabView;
    private final PluginListView installedView;
    private final PluginListView availableView;
    private final List<PluginItem> installedItems = new ArrayList<>();
    private final List<String> enabledPlugins = new ArrayList<>();
    private final List<String> compatiblePlugins = new ArrayList<>();
    private final List<PluginItem> availableItems = new ArrayList<>();

    public RusherManWindow() {
        super("RusherMan", 350, 325, 400, 325);
        INSTANCE = this;
        List<WindowContent> contents = new ArrayList<>();
        this.setMinWidth(400);
        this.setMinHeight(325);

        final TextComponent searchHelper = new TextComponent(this, "Search:");
        final TextFieldComponent searchField = new TextFieldComponent(this, "", 100, false);
        final ComboContent searchCombo = new ComboContent(this);
        searchCombo.addContent(searchHelper);
        searchCombo.addContent(searchField, ComboContent.AnchorSide.RIGHT);

        this.availableView = new PluginListView("Available", this, this.availableItems, false);
        this.installedView = new PluginListView("Installed", this, this.installedItems, true);

        final SimpleView availableTab = new SimpleView("Available", this, List.of(searchCombo, this.availableView));

        this.tabView = new TabbedView(this, List.of(this.installedView, availableTab));

        refreshLists("");

        searchField.setReturnCallback(this::refreshLists);
    }


    @Override
    public WindowView getRootView() {
        return this.tabView;
    }

    private void refreshLists(String substring) {
        installedItems.clear();
        availableItems.clear();
        compatiblePlugins.clear();
        enabledPlugins.clear();

        String filter = substring == null ? "" : substring.toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
        for (String name : listInstalled(true)) {
            this.installedItems.add(new PluginItem(name, true, this.installedView));
            enabledPlugins.add(name);
        }
        for (String name : listInstalled(false)) {
            this.installedItems.add(new PluginItem(name, true, this.installedView));
        }
        for (String name : listAllRemoteNames()) {
            String check = name.toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
            if (filter.isEmpty() || check.contains(filter)) {
                if (isCompatible(name)) {
                    compatiblePlugins.add(name);
                    this.availableItems.add(new PluginItem(name, false, this.availableView));
                }
            }
        }
        for (String name : listAllRemoteNames()) {
            String check = name.toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
            if (filter.isEmpty() || check.contains(filter)) {
                if (!isCompatible(name)) {
                    this.availableItems.add(new PluginItem(name, false, this.availableView));
                }
            }
        }
    }


    class PluginItem extends ListItemContent {
        private final String name;
        private final boolean isInstalled;

        public PluginItem(String name, boolean isInstalled, ListView<PluginItem> view) {
            super(RusherManWindow.this, view);
            this.name = name;
            this.isInstalled = isInstalled;
            this.contextMenu.add(new ContextAction("Get Info", () -> {
                RusherHackAPI.getWindowManager().popupWindow(new InfoPopup(isInstalled, name));
            }));
        }

        @Override
        public String getAsString(ListView<?>.Column column) {
            if (column.getName().equalsIgnoreCase("Plugin Name")) {
                return this.name;
            } else if (column.getName().equalsIgnoreCase("Status")) {
                if (enabledPlugins.contains(name) && this.isInstalled) {
                    return "Enabled";
                } else if (this.isInstalled) {
                    return "Disabled";
                }
                if (compatiblePlugins.contains(name)) {
                    return "Compatible";
                } else {
                    return "Not Compatible";
                }
            }
            return "null";
        }
    }


    class PluginListView extends ListView<PluginItem> {
        public PluginListView(String name, Window window, List<PluginItem> items, boolean installed) {
            super(name, window, items);
            this.addColumn("Plugin Name");
            this.addColumn("Status", 0.4);

        }
    }

    class InfoPopup extends PopupWindow {

        public InfoPopup(boolean isInstalled, String name) {
            super("Plugin info", RusherManWindow.this, 360, 240);
            List<WindowContent>infoContent = new ArrayList<>();
            ScrollableView infoView = new ScrollableView("Infos", this, infoContent);
            ParagraphComponent infoComponent = new ParagraphComponent(this, getInfo(name, isInstalled));
            infoComponent.setColor(0xfb9bff);
            infoContent.add(infoComponent);
            this.addContent(infoView);
            if (isInstalled) {
                infoContent.add(new ButtonComponent(this, "Uninstall", () -> {
                    RusherHackAPI.getWindowManager().popupWindow(new ConfirmationWindow("Are you sure you want to uninstall " + name, () -> {
                        try {
                            if (removePlugin(name)) {
                                this.onClose();
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }));
                }));
                for (File file : getFiles()) {
                    if (!file.getName().endsWith(".jar") && !file.getName().endsWith(".jar.disabled")) continue;
                    try {
                        if (parsePluginJarMetadata(file).getName().equalsIgnoreCase(name)) {
                            if (file.getName().endsWith(".jar.disabled")) {
                                infoContent.add(new ButtonComponent(this, "Enable", () -> {
                                    if (enablePlugin(name)) {
                                        RusherHackAPI.getWindowManager().popupWindow(new SuccessWindow());
                                    }
                                    this.onClose();
                                }));
                            } else if (parsePluginJarMetadata(file).getMixinConfigs() == null) {
                                infoContent.add(new ButtonComponent(this, "Disable", () -> {
                                    if (disablePlugin(name)) {
                                        RusherHackAPI.getWindowManager().popupWindow(new SuccessWindow());
                                    }
                                    this.onClose();
                                }));
                            }
                        }

                    } catch (Exception ignored) {
                    }
                }
            } else if (isCompatible(name)) {
                infoContent.add(new ButtonComponent(this, "Install", () -> {
                    try {
                        RusherHackAPI.getWindowManager().popupWindow(new ConfirmationWindow("Are you sure you want to Install " + name + installPlugin(name), null));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
        }


    }

    class ConfirmationWindow extends PopupWindow {

        public ConfirmationWindow(String text, Runnable command) {
            super("Confirm", RusherManWindow.this, 600, 150);
            List<WindowContent> content = new ArrayList<>();
            ScrollableView view = new ScrollableView("Confirm", this, content);
            ParagraphComponent paragraph = new ParagraphComponent(this, text);
            paragraph.setColor(0x00ffff);
            content.add(paragraph);
            if (guiLinks.size() == 0) {
                content.add(new ButtonComponent(this, "Confirm", () -> {
                    command.run();
                    RusherHackAPI.getWindowManager().popupWindow(new SuccessWindow());
                    this.onClose();
                }));
            } else {
                if (guiLinks.size() == 1) {
                    content.add(new ButtonComponent(this, "Download", () -> {
                        downloadPlugin(guiLinks.getFirst());
                        RusherHackAPI.getWindowManager().popupWindow(new SuccessWindow());
                        this.onClose();
                    }));
                } else {
                    int i = 1;
                    for (String url : guiLinks) {
                        content.add(new ButtonComponent(this, "Download from link " + i++, () -> {
                            downloadPlugin(url);
                            RusherHackAPI.getWindowManager().popupWindow(new SuccessWindow());
                            this.onClose();
                        }));
                    }
                }
            }

            content.add(new ButtonComponent(this, "Cancel", this::onClose));

            this.addContent(view);
        }
    }

    class SuccessWindow extends PopupWindow {

        public SuccessWindow() {
            super("Success!", RusherManWindow.this, 300, 150);
            List<WindowContent> content = new ArrayList<>();
            ScrollableView view = new ScrollableView("SuccessInfo", this, content);
            ParagraphComponent paragraph = new ParagraphComponent(this, "Success! For changes to take place, you need to reload plugins. You can do that by clicking the reload button, or typing *reload.");
            paragraph.setColor(0x00ff00);
            content.add(paragraph);

            content.add(new ButtonComponent(this, "Reload", () -> {
                reloadPlugins();
                this.onClose();
            }));

            content.add(new ButtonComponent(this, "OK", this::onClose));

            this.addContent(view);
        }
    }


    // below are utils for managing plugins
    public String getPluginDir() {
        return mc.gameDirectory.toPath() + "/rusherhack/plugins";
    }

    public File[] getFiles() {
        File dir = new File(getPluginDir());
        return dir.listFiles(File::isFile);
    }

    public List<String> listInstalled(boolean enabled) {
        List<String> outputComponents = new ArrayList<>();
        for (File file : getFiles()) {
            try {
                if (file.getName().endsWith(".jar") && enabled) {
                    outputComponents.add(parsePluginJarMetadata(file).getName());
                } else if (file.getName().endsWith(".jar.disabled") && !enabled) {
                    outputComponents.add(parsePluginJarMetadata(file).getName());
                }
            } catch (Exception e) {
                outputComponents.add(String.valueOf(e));
            }
        }
        return outputComponents;
    }

    private List<String> listAllRemoteNames() {
        List<String> names = new ArrayList<>();
        for (JsonElement plugin : getRemotePluginsJson()) {
            JsonObject obj = plugin.getAsJsonObject();
            names.add(obj.get("name").getAsString());
        }
        return names;
    }

    private JsonArray getRemotePluginsJson() {
        Path path = mc.gameDirectory.toPath().resolve("rusherhack/cache/plugins-and-themes.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return root.getAsJsonArray("plugins");
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    private boolean isCompatible(String pluginName) {
        for (JsonElement plugin : getRemotePluginsJson()) {
            JsonObject obj = plugin.getAsJsonObject();
            if (!obj.get("name").getAsString().equalsIgnoreCase(pluginName)) continue;
            return isVersionCompatible(obj.get("mc_versions").getAsString());
        }
        return false;
    }

    private String getInfo(String name, boolean isInstalled) {
        if (!isInstalled) {
            for (JsonElement plugin : getRemotePluginsJson()) {
                JsonObject obj = plugin.getAsJsonObject();
                if (!obj.get("name").getAsString().equalsIgnoreCase(name)) continue;
                return obj.get("name").getAsString() + ":\n\n\n"
                        + "Author: " + obj.getAsJsonObject("creator").get("name").getAsString() + "\n\n"
                        + "Description: " + obj.get("description").getAsString() + "\n\n"
                        + "Plugin Repo: https://github.com/" + obj.get("repo").getAsString() + "\n\n"
                        + "Supported versions: " + obj.get("mc_versions").getAsString() + "\n\n"
                        + "Supports this version: " + isVersionCompatible(obj.get("mc_versions").getAsString()) + "\n\n"
                        + "Is a core plugin: " + obj.get("is_core").getAsString() + "\n\n"
                        + "This plugin is on the plugins repo.";
            }
        } else {
            for (File file : getFiles()) {
                if (!file.getName().endsWith(".jar") && !file.getName().endsWith(".jar.disabled")) continue;
                try {
                    if (parsePluginJarMetadata(file).getName().equalsIgnoreCase(name)) {

                        String isDisabled = "This plugin is enabled.";
                        if (file.getName().endsWith(".jar.disabled")) {
                            isDisabled = "This plugin is disabled.";
                        }

                        String coreStatus = (parsePluginJarMetadata(file).getMixinConfigs() != null)
                                ? " is a core plugin" : " is not a core plugin";
                        return name + "\n\n\n" +
                                "Plugin Name: " + name + "\n\n" +
                                "Description: " + parsePluginJarMetadata(file).getDescription() + "\n\n" +
                                "url: " + parsePluginJarMetadata(file).getURL() + "\n\n" +
                                "Authors: " + String.join(", ", parsePluginJarMetadata(file).getAuthors()) + "\n\n" +
                                isDisabled + "\n\n" +
                                name + coreStatus + "\n\n" +
                                "Jarfile Name: " + file.getName() + "\n\n" +
                                "This plugin is installed locally.";
                    }
                } catch (Exception e) {
                    return e.toString();
                }
            }
        }
        return "This plugin no longer exists on your computer. Please run *reload to re-sync rusherman.";
    }


    private boolean isVersionCompatible(String range) {
        String supported = net.minecraft.SharedConstants.getCurrentVersion().getName();
        if (range.contains("-")) {
            String[] parts = range.split("-");
            return compareVersion(supported, parts[0]) >= 0 && compareVersion(supported, parts[1]) <= 0;
        }
        return supported.equals(range);
    }

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

    private boolean removePlugin(String pluginName) throws Exception {
        File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
        if (pluginFile.isFile()) {
            pluginFile.delete();
            return true;
        }
        return false;
    }

    private String fileFromName(String pluginName) throws Exception {
        for (File file : getFiles()) {
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled")) {
                if (Objects.equals(parsePluginJarMetadata(file).getName(), pluginName)) {
                    return file.getName();
                }
            }
        }
        return "No plugin found with name " + pluginName;
    }

    private void reloadPlugins() {
        try {
            RusherHackAPI.getCommandManager().getDispatcher().dispatch(command -> true, "reload");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // good coding practices frfr thanks john
        mc.schedule(() -> {
            mc.schedule(() -> {
                mc.setScreen(RusherHackAPI.getThemeManager().getWindowsScreen());
            });
        });

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

    private String installPlugin(String PluginName) throws Exception {
        StringBuilder outputString = new StringBuilder();
        guiLinks.clear();
            List<String>possibleLinks =  getReleaseLink(PluginName);
            if (possibleLinks != null) {
                if (possibleLinks.size() == 1) {
                    outputString.append("\n\nDownloading from ").append(possibleLinks.getFirst()).append("\n\n");
                    guiLinks.add(possibleLinks.getFirst());
                    outputString.append("Warning: None of these plugins have been verified or confirmed to be safe. Use at your own risk!\n");
                    return outputString.append("\nclick Download to continue download").toString();

                } else {
                    outputString.append("\n\nDetected Multiple links\n");
                    int i = 1;
                    for (String link : possibleLinks) {
                        guiLinks.add(link);
                        outputString.append(i++).append(". ").append(link).append("\n");
                    }
                    outputString.append("\n\nWarning: None of these plugins have been verified or confirmed to be safe. Use at your own risk!\n\n");
                    return outputString.append("click Download <number> to continue download").toString();
                }
            }
            return "Something went wrong";
        }

    private boolean downloadPlugin(String url) {
        try {
            Path dir = mc.gameDirectory.toPath().resolve("rusherhack/plugins");
            Files.createDirectories(dir);
            String fileName = Paths.get(new URL(url).getPath()).getFileName().toString();
            Path filePath = dir.resolve(fileName);
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean disablePlugin(String pluginName) {
        try {
        File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
        if (pluginFile.isFile()) {
            if (parsePluginJarMetadata(pluginFile).getMixinConfigs() != null) {
                return false;
            }
            if (pluginFile.getName().endsWith(".jar")) {
                pluginFile.renameTo(new File(pluginFile.getParent(), pluginFile.getName().replaceFirst(".jar", ".jar.disabled")));
                return true;
            } else if (pluginFile.getName().endsWith(".jar.disabled")) {
                return true;
            }
        }
        return false;
    } catch (Exception e) {
        return false;
        }
    }


    private boolean enablePlugin(String pluginName) {
        try {
        File pluginFile = new File(getPluginDir(), fileFromName(pluginName));
        if (pluginFile.isFile()) {
            if (pluginFile.getName().endsWith(".jar.disabled")) {
                pluginFile.renameTo(new File(pluginFile.getParent(), pluginFile.getName().replaceFirst("\\.disabled$", "")));
                return true;
            } else if (pluginFile.getName().endsWith(".jar")) {
                return true;
            }
        }
        return false;
    } catch (Exception e) {
        return false;
        }
    }


}
