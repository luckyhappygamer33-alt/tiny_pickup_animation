package net.cat_metalhead.tiny_pickup_animation;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.FloatSlider;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class ModConfig {
        public static final ConfigClassHandler<ModConfig> HANDLER = ConfigClassHandler.createBuilder(ModConfig.class)
                        .id(Identifier.of("tiny_pickup_animation", "config"))
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
        public boolean crafterAnimationEnabled = true;
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
        // @dev.isxander.yacl3.config.v2.api.autogen.IntField(min = 0, max = 200)
        @IntSlider(min = 0, max = 100, step = 1)
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

        @SerialEntry
        @AutoGen(category = "animation")
        @FloatSlider(min = 0.0f, max = 2.0f, step = 0.05f, format = "%.2f")
        public float bounceScale = 0.60f;

        @SerialEntry
        @AutoGen(category = "animation")
        @FloatSlider(min = 0.5f, max = 20.0f, step = 0.5f)
        public float animationDuration = 6.0f;

        public static ModConfig get() {
                return HANDLER.instance();
        }

}
