package com.trusted.systemnbteditor;

import com.trusted.systemnbteditor.util.NbtIoUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.io.File;

public class TestChestLoad {
    public static void printChest() {
        File folder = new File("C:\\Users\\wasie\\AppData\\Roaming\\ModrinthApp\\profiles\\1.21.11 True NBT Power\\config\\system_nbt_editor\\client_chest");
        File file = new File(folder, "page0.nbt");
        System.out.println("File exists: " + file.exists());
        if (file.exists()) {
            NbtCompound root = NbtIoUtils.readNbt(file.toPath());
            System.out.println(root.asString());
        }
    }
}
