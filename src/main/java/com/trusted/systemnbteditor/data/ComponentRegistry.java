package com.trusted.systemnbteditor.data;

import java.util.*;

/**
 * Registry of all Minecraft 1.21.11 data components.
 * Based on https://minecraft.wiki/w/Data_component_format
 */
public class ComponentRegistry {

    public record ComponentInfo(String key, String displayName, String defaultValue) {}

    private static final List<ComponentInfo> COMPONENTS = new ArrayList<>();

    static {
        COMPONENTS.add(new ComponentInfo("minecraft:custom_data", "Custom Data", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:max_stack_size", "Max Stack Size", "64"));
        COMPONENTS.add(new ComponentInfo("minecraft:max_damage", "Max Damage", "0"));
        COMPONENTS.add(new ComponentInfo("minecraft:damage", "Damage", "0"));
        COMPONENTS.add(new ComponentInfo("minecraft:unbreakable", "Unbreakable", "{\"show_in_tooltip\":true}"));
        COMPONENTS.add(new ComponentInfo("minecraft:custom_name", "Custom Name", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:item_name", "Item Name", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:lore", "Lore", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:rarity", "Rarity", "\"common\""));
        COMPONENTS.add(new ComponentInfo("minecraft:enchantments", "Enchantments", "{\"levels\":{}}"));
        COMPONENTS.add(new ComponentInfo("minecraft:can_place_on", "Can Place On", "{\"predicates\":[]}"));
        COMPONENTS.add(new ComponentInfo("minecraft:can_break", "Can Break", "{\"predicates\":[]}"));
        COMPONENTS.add(new ComponentInfo("minecraft:attribute_modifiers", "Attribute Modifiers", "{\"modifiers\":[]}"));
        COMPONENTS.add(new ComponentInfo("minecraft:custom_model_data", "Custom Model Data", "0"));
        COMPONENTS.add(new ComponentInfo("minecraft:repair_cost", "Repair Cost", "0"));
        COMPONENTS.add(new ComponentInfo("minecraft:enchantment_glint_override", "Enchantment Glint", "true"));
        COMPONENTS.add(new ComponentInfo("minecraft:food", "Food", "{\"nutrition\":0,\"saturation\":0}"));
        COMPONENTS.add(new ComponentInfo("minecraft:tool", "Tool", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:stored_enchantments", "Stored Enchantments", "{\"levels\":{}}"));
        COMPONENTS.add(new ComponentInfo("minecraft:banner_patterns", "Banner Patterns", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:base_color", "Base Color", "\"white\""));
        COMPONENTS.add(new ComponentInfo("minecraft:pot_decorations", "Pot Decorations", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:container", "Container", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:block_state", "Block State", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:bees", "Bees", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:lock", "Lock", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:bundle_contents", "Bundle Contents", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:charged_projectiles", "Charged Projectiles", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:container_loot", "Container Loot", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:debug_stick_state", "Debug Stick State", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:dyed_color", "Dyed Color", "{\"rgb\":0}"));
        COMPONENTS.add(new ComponentInfo("minecraft:entity_data", "Entity Data", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:bucket_entity_data", "Bucket Entity Data", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:firework_explosion", "Firework Explosion", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:fireworks", "Fireworks", "{\"explosions\":[]}"));
        COMPONENTS.add(new ComponentInfo("minecraft:instrument", "Instrument", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:jukebox_playable", "Jukebox Playable", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:lodestone_tracker", "Lodestone Tracker", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:map_color", "Map Color", "0"));
        COMPONENTS.add(new ComponentInfo("minecraft:map_decorations", "Map Decorations", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:map_id", "Map ID", "0"));
        COMPONENTS.add(new ComponentInfo("minecraft:block_entity_data", "Block Entity Data", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:trim", "Trim", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:ominous_bottle_amplifier", "Ominous Bottle Amplifier", "0"));
        COMPONENTS.add(new ComponentInfo("minecraft:suspicious_stew_effects", "Suspicious Stew Effects", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:writable_book_content", "Writable Book Content", "{\"pages\":[]}"));
        COMPONENTS.add(new ComponentInfo("minecraft:written_book_content", "Written Book Content", "{\"pages\":[],\"title\":\"\",\"author\":\"\"}"));
        COMPONENTS.add(new ComponentInfo("minecraft:profile", "Profile", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:note_block_sound", "Note Block Sound", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:recipes", "Recipes", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:tooltip_style", "Tooltip Style", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:item_model", "Item Model", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:equippable", "Equippable", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:glider", "Glider", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:tooltip_display", "Tooltip Display", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:damage_resistant", "Damage Resistant", "{\"types\":\"#minecraft:is_fire\"}"));
        COMPONENTS.add(new ComponentInfo("minecraft:damage_type", "Damage Type", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:use_effects", "Use Effects", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:consumable", "Consumable", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:minimum_attack_charge", "Minimum Attack Charge", "0.0"));
        COMPONENTS.add(new ComponentInfo("minecraft:swing_animation", "Swing Animation", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:use_remainder", "Use Remainder", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:repairable", "Repairable", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:death_protection", "Death Protection", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:enchantable", "Enchantable", "{\"value\":1}"));
        COMPONENTS.add(new ComponentInfo("minecraft:attack_range", "Attack Range", "3.0"));
        COMPONENTS.add(new ComponentInfo("minecraft:blocks_attacks", "Blocks Attacks", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:break_sound", "Break Sound", "\"\""));
        COMPONENTS.add(new ComponentInfo("minecraft:potion_contents", "Potion Contents", "{\"potion\":\"minecraft:water\"}"));
        
        // Added missing from image
        COMPONENTS.add(new ComponentInfo("minecraft:dye", "Dye", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:intangible_projectile", "Intangible Projectile", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:kinetic_weapon", "Kinetic Weapon", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:piercing_weapon", "Piercing Weapon", "{}"));
        COMPONENTS.add(new ComponentInfo("minecraft:potion_duration_scale", "Potion Duration Scale", "1.0"));
        COMPONENTS.add(new ComponentInfo("minecraft:provides_banner_patterns", "Provides Banner Patterns", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:provides_trim_material", "Provides Trim Material", "[]"));
        COMPONENTS.add(new ComponentInfo("minecraft:use_cooldown", "Use Cooldown", "{\"seconds\":0.0}"));
        COMPONENTS.add(new ComponentInfo("minecraft:weapon", "Weapon", "{}"));



        // Sort alphabetically by display name
        COMPONENTS.sort(Comparator.comparing(ComponentInfo::displayName));
    }

    public static List<ComponentInfo> getAllComponents() {
        return Collections.unmodifiableList(COMPONENTS);
    }

    public static ComponentInfo getComponent(String key) {
        return COMPONENTS.stream()
                .filter(c -> c.key().equals(key))
                .findFirst()
                .orElse(null);
    }
}
