package org.tilley;


//ty john for this code
//john if ur reading this please add plugin accessors to rh api <3


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.minecraft.DetectedVersion;
import org.rusherhack.client.api.config.JsonConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author John200410 12/18/2023
 * mildly modified by tilley with permission
 */

public final class PluginMetadata {
    private final String pluginClass;
    private String name = null;
    private String version = null;
    private String description = null;
    private String url = null;
    private String[] authors = null;
    private String[] supportedMinecraftVersions = new String[]{DetectedVersion.BUILT_IN.getName()};
    private String[] mixinConfigs = null;

    public PluginMetadata(String pluginClass) {
        this.pluginClass = pluginClass;
    }

    public String getMainClass() {
        return pluginClass;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getURL() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public String[] getSupportedMinecraftVersions() {
        return supportedMinecraftVersions;
    }

    public String[] getAuthors() {
        return authors;
    }

    public String[] getMixinConfigs() {
        return mixinConfigs;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setSupportedMinecraftVersions(String[] supportedMinecraftVersions) {
        this.supportedMinecraftVersions = supportedMinecraftVersions;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public void setAuthors(String[] authors) {
        this.authors = authors;
    }

    public void setMixinConfigs(String[] mixinConfigs) {
        this.mixinConfigs = mixinConfigs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PluginMetadata) obj;
        return Objects.equals(this.pluginClass, that.pluginClass) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.version, that.version) &&
                Arrays.equals(this.supportedMinecraftVersions, that.supportedMinecraftVersions) &&
                Objects.equals(this.description, that.description) &&
                Arrays.equals(this.authors, that.authors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginClass, name, version, Arrays.hashCode(supportedMinecraftVersions), description, Arrays.hashCode(authors));
    }

    @Override
    public String toString() {
        return "CorePluginMetadata[" +
                "pluginClass=" + pluginClass + ", " +
                "name=" + name + ", " +
                "version=" + version + ", " +
                "supportedMinecraftVersions=" + Arrays.toString(supportedMinecraftVersions) + ", " +
                "description=" + description + ", " +
                "url=" + url + ", " +
                "authors=" + Arrays.toString(authors) + ", " +
                "mixinConfigs=" + Arrays.toString(mixinConfigs) + ']';
    }

    public JsonObject toJson() {
        final JsonObject obj = new JsonObject();

        obj.addProperty("Plugin-Class", this.pluginClass);
        obj.addProperty("Name", this.name);

        if (this.version != null) {
            obj.addProperty("Version", this.version);
        }
        if (this.description != null) {
            obj.addProperty("Description", this.description);
        }
        if (this.url != null) {
            obj.addProperty("URL", this.url);
        }

        final JsonArray authorsArray = new JsonArray();
        for (String author : this.authors) {
            authorsArray.add(author);
        }
        obj.add("Authors", authorsArray);

        final JsonArray minecraftVersionsArray = new JsonArray();
        for (String minecraftVersion : this.supportedMinecraftVersions) {
            minecraftVersionsArray.add(minecraftVersion);
        }
        obj.add("Minecraft-Versions", minecraftVersionsArray);

        final JsonArray mixinConfigsArray = new JsonArray();
        for (String mixinConfig : this.mixinConfigs) {
            mixinConfigsArray.add(mixinConfig);
        }
        obj.add("Mixin-Configs", mixinConfigsArray);

        return obj;
    }
    public static PluginMetadata parsePluginJarMetadata(File pluginFile) throws Exception {
        try(final ZipFile zipFile = new ZipFile(pluginFile)) {
            final ZipEntry entry = zipFile.getEntry("rusherhack-plugin.json");

            if(entry == null) {
                throw new Exception("Plugin metadata not found");
            }

            try(final InputStream is = zipFile.getInputStream(entry)) {
                final JsonReader jsonReader = new JsonReader(new InputStreamReader(is));
                final JsonObject jsonObj = JsonConfiguration.GSON.fromJson(jsonReader, JsonObject.class);
                return parsePluginMetadata(jsonObj);
            }
        }
    }

    public static PluginMetadata parsePluginMetadata(JsonObject jsonObj) {

        //required
        final String pluginClass = jsonObj.getAsJsonPrimitive("Plugin-Class").getAsString();

        //metadata
        final PluginMetadata metadata = new PluginMetadata(pluginClass);
        if(jsonObj.has("Name")) {
            metadata.setName(jsonObj.getAsJsonPrimitive("Name").getAsString());
        }

        if(jsonObj.has("Version")) {
            metadata.setVersion(jsonObj.getAsJsonPrimitive("Version").getAsString());
        }

        if(jsonObj.has("Description")) {
            metadata.setDescription(jsonObj.getAsJsonPrimitive("Description").getAsString());
        }

        if(jsonObj.has("URL")) {
            metadata.setURL(jsonObj.getAsJsonPrimitive("URL").getAsString());
        }

        if(jsonObj.has("Authors")) {
            metadata.setAuthors(JsonConfiguration.GSON.fromJson(jsonObj.getAsJsonArray("Authors"), String[].class));
        }

        if(jsonObj.has("Minecraft-Versions")) {
            metadata.setSupportedMinecraftVersions(JsonConfiguration.GSON.fromJson(jsonObj.getAsJsonArray("Minecraft-Versions"), String[].class));
        }

        if(jsonObj.has("Mixin-Configs")) {
            metadata.setMixinConfigs(JsonConfiguration.GSON.fromJson(jsonObj.getAsJsonArray("Mixin-Configs"), String[].class));
        }

        return metadata;
    }
}
