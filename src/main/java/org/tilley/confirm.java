package org.tilley;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.client.api.utils.ChatUtils;
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
    private void confirm(Optional<Integer> option) {
        if (!RusherManCommand.downloadLinks.isEmpty()) {
            if (RusherManCommand.downloadLinks.size() == 1) {
                ChatUtils.print("Downloading plugin...");
                downloadPlugin(RusherManCommand.downloadLinks.getFirst());
                RusherManCommand.downloadLinks.clear();
            } else if (option.isPresent()) {
                if (option.get() <= RusherManCommand.downloadLinks.size()) {
                    ChatUtils.print("Downloading plugin candidate " + option.get() + "...");
                    downloadPlugin(RusherManCommand.downloadLinks.get(option.get() - 1));
                    RusherManCommand.downloadLinks.clear();
                } else {
                    ChatUtils.print("Invalid option!");
                }
            } else {
                ChatUtils.print("Please select an option to download.");
            }
        } else {
            ChatUtils.print("Please use *rusherman install <plugin name> to get an action to confirm.");
        }
    }

    private void downloadPlugin(String url) {
        try {
            Path dir = mc.gameDirectory.toPath().resolve("rusherhack/plugins");
            Files.createDirectories(dir);
            String fileName = Paths.get(new URL(url).getPath()).getFileName().toString();
            Path filePath = dir.resolve(fileName);
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            ChatUtils.print(makeComponent("Downloading plugin successful!", 0x3bc200));
        } catch (IOException e) {
            ChatUtils.print(makeComponent("Downloading plugin failed! " + e, 0xff0000));
        }
    }

}
