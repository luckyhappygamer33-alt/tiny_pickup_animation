package net.cat_metalhead.tiny_pickup_animation;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.cat_metalhead.tiny_pickup_animation.ModConfig.AnimationMode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class ModConfig {
        public static final ConfigClassHandler<ModConfig> HANDLER = ConfigClassHandler.createBuilder(ModConfig.class)
                        .id(new Identifier("tiny_pickup_animation", "config"))
                        .serializer(config -> GsonConfigSerializerBuilder.create(config)
                                        .setPath(FabricLoader.getInstance().getConfigDir()
                                                        .resolve("tiny_pickup_animation.json"))
                                        .build())
                        .build();

        public enum AnimationMode {
                CUSTOM,
                VANILLA,
                DISABLED
        }

        // Global
        @SerialEntry
        @AutoGen(category = "general")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean enabled = true;

        // Hotbar
        @SerialEntry
        @AutoGen(category = "hotbar")
        @dev.isxander.yacl3.config.v2.api.autogen.EnumCycler
        public AnimationMode hotbarAnimationMode = AnimationMode.CUSTOM;
        @SerialEntry
        @AutoGen(category = "hotbar")
        @dev.isxander.yacl3.config.v2.api.autogen.EnumCycler
        public AnimationMode pickBlockAnimationMode = AnimationMode.CUSTOM;
        @SerialEntry
        @AutoGen(category = "hotbar")
        @dev.isxander.yacl3.config.v2.api.autogen.EnumCycler
        public AnimationMode itemStateChangedAnimationMode = AnimationMode.CUSTOM;

        // Inventory
        @SerialEntry
        @AutoGen(category = "inventory")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean inventoryAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "inventory")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean containersAnimationEnabled = true;

        // Workstations
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean craftingAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean furnaceAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean brewingStandAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.IntField(min = 0, max = 200)
        public int brewingStandCascadeDelay = 2;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean enchantingTableAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean anvilAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean grindstoneAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean smithingTableAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean stonecutterAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean loomAnimationEnabled = true;
        @SerialEntry
        @AutoGen(category = "workstations")
        @dev.isxander.yacl3.config.v2.api.autogen.Boolean
        public boolean cartographyTableAnimationEnabled = true;

        public static ModConfig get() {
                return HANDLER.instance();
        }

}
