package com.trusted.systemnbteditor.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.util.HashMap;
import java.util.Map;

public class ComponentIconUtil {
    private static final Map<String, ItemStack> ICON_MAP = new HashMap<>();

    static {
        initIconMap();
    }

    public static ItemStack getIcon(String key) {
        return ICON_MAP.getOrDefault(key, new ItemStack(Items.BARRIER));
    }

    private static void initIconMap() {
        ICON_MAP.put("minecraft:attack_range", Items.ARROW.getDefaultStack());
        ICON_MAP.put("minecraft:attribute_modifiers", Items.CHAIN_COMMAND_BLOCK.getDefaultStack());
        ICON_MAP.put("minecraft:banner_patterns", Items.WHITE_BANNER.getDefaultStack());
        ICON_MAP.put("minecraft:base_color", Items.WHITE_DYE.getDefaultStack());
        ICON_MAP.put("minecraft:bees", Items.BEE_NEST.getDefaultStack());
        ICON_MAP.put("minecraft:block_entity_data", Items.SPAWNER.getDefaultStack());
        ICON_MAP.put("minecraft:block_state", Items.GRASS_BLOCK.getDefaultStack());
        ICON_MAP.put("minecraft:blocks_attacks", Items.SHIELD.getDefaultStack());
        ICON_MAP.put("minecraft:break_sound", Items.NOTE_BLOCK.getDefaultStack());
        ICON_MAP.put("minecraft:bucket_entity_data", Items.AXOLOTL_BUCKET.getDefaultStack());
        ICON_MAP.put("minecraft:bundle_contents", Items.BUNDLE.getDefaultStack());
        ICON_MAP.put("minecraft:can_break", Items.IRON_PICKAXE.getDefaultStack());
        ICON_MAP.put("minecraft:can_place_on", Items.GRASS_BLOCK.getDefaultStack());
        ICON_MAP.put("minecraft:charged_projectiles", Items.CROSSBOW.getDefaultStack());
        ICON_MAP.put("minecraft:consumable", Items.GOLDEN_APPLE.getDefaultStack());
        ICON_MAP.put("minecraft:container", Items.SHULKER_BOX.getDefaultStack());
        ICON_MAP.put("minecraft:container_loot", Items.CHEST.getDefaultStack());
        ICON_MAP.put("minecraft:custom_data", Items.BARRIER.getDefaultStack());
        ICON_MAP.put("minecraft:custom_model_data", Items.DIAMOND.getDefaultStack());
        ICON_MAP.put("minecraft:custom_name", Items.NAME_TAG.getDefaultStack());
        ICON_MAP.put("minecraft:damage", Items.WOODEN_SWORD.getDefaultStack());
        ICON_MAP.put("minecraft:damage_resistant", Items.NETHERITE_SCRAP.getDefaultStack());
        ICON_MAP.put("minecraft:damage_type", Items.IRON_SWORD.getDefaultStack());
        ICON_MAP.put("minecraft:death_protection", Items.TOTEM_OF_UNDYING.getDefaultStack());
        ICON_MAP.put("minecraft:debug_stick_state", Items.DEBUG_STICK.getDefaultStack());
        ICON_MAP.put("minecraft:dye", Items.LAPIS_LAZULI.getDefaultStack());
        ICON_MAP.put("minecraft:dyed_color", Items.LEATHER_CHESTPLATE.getDefaultStack());
        ICON_MAP.put("minecraft:enchantable", Items.DIAMOND_BOOTS.getDefaultStack());
        ICON_MAP.put("minecraft:enchantment_glint_override", Items.EXPERIENCE_BOTTLE.getDefaultStack());
        ICON_MAP.put("minecraft:enchantments", Items.BOOK.getDefaultStack());
        ICON_MAP.put("minecraft:entity_data", Items.ARMOR_STAND.getDefaultStack());
        ICON_MAP.put("minecraft:equippable", Items.SADDLE.getDefaultStack());
        ICON_MAP.put("minecraft:firework_explosion", Items.FIREWORK_STAR.getDefaultStack());
        ICON_MAP.put("minecraft:fireworks", Items.FIREWORK_ROCKET.getDefaultStack());
        ICON_MAP.put("minecraft:food", Items.COOKED_BEEF.getDefaultStack());
        ICON_MAP.put("minecraft:glider", Items.ELYTRA.getDefaultStack());
        ICON_MAP.put("minecraft:instrument", Items.GOAT_HORN.getDefaultStack());
        ICON_MAP.put("minecraft:intangible_projectile", Items.ARROW.getDefaultStack());
        ICON_MAP.put("minecraft:item_model", Items.EMERALD.getDefaultStack());
        ICON_MAP.put("minecraft:item_name", Items.SUNFLOWER.getDefaultStack());
        ICON_MAP.put("minecraft:jukebox_playable", Items.MUSIC_DISC_5.getDefaultStack());
        ICON_MAP.put("minecraft:kinetic_weapon", Items.TRIDENT.getDefaultStack());
        ICON_MAP.put("minecraft:lock", Items.TRIPWIRE_HOOK.getDefaultStack());
        ICON_MAP.put("minecraft:lodestone_tracker", Items.COMPASS.getDefaultStack());
        ICON_MAP.put("minecraft:lore", Items.PAPER.getDefaultStack());
        ICON_MAP.put("minecraft:map_color", Items.MAP.getDefaultStack());
        ICON_MAP.put("minecraft:map_decorations", Items.FILLED_MAP.getDefaultStack());
        ICON_MAP.put("minecraft:map_id", Items.MAP.getDefaultStack());
        ICON_MAP.put("minecraft:max_damage", Items.IRON_PICKAXE.getDefaultStack());
        ICON_MAP.put("minecraft:max_stack_size", Items.EGG.getDefaultStack());
        ICON_MAP.put("minecraft:minimum_attack_charge", Items.WOODEN_SWORD.getDefaultStack());
        ICON_MAP.put("minecraft:note_block_sound", Items.JUKEBOX.getDefaultStack());
        ICON_MAP.put("minecraft:ominous_bottle_amplifier", Items.OMINOUS_BOTTLE.getDefaultStack());
        ICON_MAP.put("minecraft:piercing_weapon", Items.ARROW.getDefaultStack());
        ICON_MAP.put("minecraft:pot_decorations", Items.FLOWER_POT.getDefaultStack());
        ICON_MAP.put("minecraft:potion_contents", Items.POTION.getDefaultStack());
        ICON_MAP.put("minecraft:potion_duration_scale", Items.SPLASH_POTION.getDefaultStack());
        ICON_MAP.put("minecraft:profile", Items.PLAYER_HEAD.getDefaultStack());
        ICON_MAP.put("minecraft:provides_banner_patterns", Items.WHITE_BANNER.getDefaultStack());
        ICON_MAP.put("minecraft:provides_trim_material", Items.NETHERITE_INGOT.getDefaultStack());
        ICON_MAP.put("minecraft:rarity", Items.NETHER_STAR.getDefaultStack());
        ICON_MAP.put("minecraft:recipes", Items.KNOWLEDGE_BOOK.getDefaultStack());
        ICON_MAP.put("minecraft:repair_cost", Items.EXPERIENCE_BOTTLE.getDefaultStack());
        ICON_MAP.put("minecraft:repairable", Items.ANVIL.getDefaultStack());
        ICON_MAP.put("minecraft:stored_enchantments", Items.ENCHANTED_BOOK.getDefaultStack());
        ICON_MAP.put("minecraft:suspicious_stew_effects", Items.SUSPICIOUS_STEW.getDefaultStack());
        ICON_MAP.put("minecraft:swing_animation", Items.WOODEN_SWORD.getDefaultStack());
        ICON_MAP.put("minecraft:tool", Items.IRON_PICKAXE.getDefaultStack());
        ICON_MAP.put("minecraft:tooltip_display", Items.ITEM_FRAME.getDefaultStack());
        ICON_MAP.put("minecraft:tooltip_style", Items.PAPER.getDefaultStack());
        ICON_MAP.put("minecraft:trim", Items.IRON_CHESTPLATE.getDefaultStack());
        ICON_MAP.put("minecraft:unbreakable", Items.BEDROCK.getDefaultStack());
        ICON_MAP.put("minecraft:use_cooldown", Items.ENDER_PEARL.getDefaultStack());
        ICON_MAP.put("minecraft:use_effects", Items.POTION.getDefaultStack());
        ICON_MAP.put("minecraft:use_remainder", Items.BUCKET.getDefaultStack());
        ICON_MAP.put("minecraft:weapon", Items.DIAMOND_SWORD.getDefaultStack());
        ICON_MAP.put("minecraft:writable_book_content", Items.WRITABLE_BOOK.getDefaultStack());
        ICON_MAP.put("minecraft:written_book_content", Items.WRITTEN_BOOK.getDefaultStack());
        
        // Manual Item Mappings
        ICON_MAP.put("minecraft:shield", Items.SHIELD.getDefaultStack());
        ICON_MAP.put("minecraft:item_frame", Items.ITEM_FRAME.getDefaultStack());
    }
}
