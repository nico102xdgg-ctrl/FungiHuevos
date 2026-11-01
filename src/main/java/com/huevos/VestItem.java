package com.huevos;

import net.minecraft.world.item.Item;

public class VestItem extends Item {
    public final String color;
    public VestItem(String color) {
        super(new Item.Properties().stacksTo(1));
        this.color = color;
    }
}
