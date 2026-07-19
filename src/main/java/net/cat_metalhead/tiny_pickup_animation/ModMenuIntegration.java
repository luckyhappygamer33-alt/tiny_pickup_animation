package net.cat_metalhead.tiny_pickup_animation;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ModConfig.HANDLER.generateGui().generateScreen(parent);
    }
}
