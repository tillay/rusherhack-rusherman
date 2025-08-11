package org.tilley.rusherman;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;

public class ComponentUtils {
    public static Component makeComponent(String text, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    public static Component linkify(String text, String URL, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, URL)));
    }

    public static Component disclaimer() {
        return makeComponent("Warning: None of these plugins have been verified or confirmed to be safe. Use at your own risk!\n", 0xe8cd00);
    }

}
