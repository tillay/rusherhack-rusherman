package org.tilley;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;
import static org.tilley.ComponentUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class confirm extends Command {
    public confirm() {
        super("confirm", "confirm installation");
    }

    @CommandExecutor()
    @CommandExecutor.Argument("option")
    private Component confirm(Optional<Integer> option) {
        MutableComponent outputComponent = (MutableComponent) makeComponent("", 0xffffff);
        if (!RusherManCommand.downloadLinks.isEmpty()) {
            if (RusherManCommand.downloadLinks.size() == 1) {
                outputComponent.append(makeComponent("Downloading plugin...\n", 0xffffff));
                outputComponent.append(downloadPlugin(RusherManCommand.downloadLinks.getFirst()));
                RusherManCommand.downloadLinks.clear();
            } else if (option.isPresent()) {
                if (option.get() <= RusherManCommand.downloadLinks.size()) {
                    outputComponent.append(makeComponent("Downloading plugin candidate " + option.get() + "...\n", 0xffffff));
                    outputComponent.append(downloadPlugin(RusherManCommand.downloadLinks.get(option.get() - 1)));
                    RusherManCommand.downloadLinks.clear();
                } else {
                    outputComponent.append(makeComponent("Invalid option!", 0x00ffff));
                }
            } else {
                outputComponent.append(makeComponent("Please select an option to download.", 0x00ffff));
            }
        } else {
            outputComponent.append(makeComponent("Please use *rusherman install <plugin name> to get an action to confirm.", 0xff0000));
        }
        return outputComponent;
    }

    private Component downloadPlugin(String url) {
        try {
            Path dir = mc.gameDirectory.toPath().resolve("rusherhack/plugins");
            Files.createDirectories(dir);
            String fileName = Paths.get(new URL(url).getPath()).getFileName().toString();
            Path filePath = dir.resolve(fileName);
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return makeComponent("Downloading plugin successful!", 0x3bc200);
        } catch (IOException e) {
            return makeComponent("Downloading plugin failed! " + e, 0xff0000);
        }
    }

}
