package org.tilley;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import org.rusherhack.client.api.utils.ChatUtils;

public class ComponentUtils {
    public static Component makeComponent(String text, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    public static Component linkify(String text, String URL, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, URL)));
    }

    public static void disclaimer() {
        ChatUtils.print(makeComponent("Warning: None of these plugins have been verified or confirmed to be safe. Use at your own risk!", 0xe8cd00));
        ChatUtils.print(makeComponent("These plugins are not affiliated with or endorsed by Rusher Development LLC.", 0x8700ff));
    }

}
