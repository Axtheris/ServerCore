package net.axther.serverCore.pet.model;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

/**
 * Isolated helper for Model Engine API calls.
 * This class is only classloaded when MEG is confirmed present at runtime,
 * keeping all MEG imports in one place to avoid ClassNotFoundException.
 */
public final class ModelEngineHook {

    private ModelEngineHook() {}

    /**
     * Applies a MEG model to the given entity (typically an armor stand).
     * @return true if model applied successfully, false if blueprint not found
     */
    public static boolean applyModel(Entity entity, String modelId) {
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        if (activeModel == null) return false;

        ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
        modeledEntity.addModel(activeModel, true);
        modeledEntity.setBaseEntityVisible(false);
        return true;
    }

    /**
     * Plays a named animation on the entity's first active model.
     */
    public static void playAnimation(Entity entity, String animationName) {
        ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
        if (modeledEntity == null) return;

        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.getAnimationHandler().playAnimation(animationName, 0.25, 0.25, 1.0, false);
            break; // only first model
        }
    }

    /**
     * Removes the modeled entity and all its models from the given entity.
     */
    public static void removeModel(Entity entity) {
        ModelEngineAPI.removeModeledEntity(entity);
    }

    /**
     * Checks if Model Engine plugin is loaded.
     */
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("ModelEngine") != null;
    }
}
