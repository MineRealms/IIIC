package cn.minerealms.iic.integration.gunmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration manager for Gun Mod workbench recipe energy costs.
 * Allows customization of energy requirements per recipe via JSON config.
 */
public class GunModRecipeConfig {

    private static final String CONFIG_FILE = "config/gunmod-workbench-energy.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Default costs
    public static final int DEFAULT_AMMO_COST = 512;      // 512 EU for ammo
    public static final int DEFAULT_WEAPON_COST = 2048;   // 2048 EU for weapons
    public static final int DEFAULT_AMMO_TIME = 60;       // 3 seconds (60 ticks)
    public static final int DEFAULT_WEAPON_TIME = 240;    // 12 seconds (240 ticks)

    // Recipe-specific overrides
    private static final Map<ResourceLocation, RecipeCost> RECIPE_COSTS = new HashMap<>();

    // Category defaults
    private static int ammoCost = DEFAULT_AMMO_COST;
    private static int weaponCost = DEFAULT_WEAPON_COST;
    private static int ammoTime = DEFAULT_AMMO_TIME;
    private static int weaponTime = DEFAULT_WEAPON_TIME;

    /**
     * Recipe cost data holder
     */
    public static class RecipeCost {
        public int energyCost;
        public int craftingTime;

        public RecipeCost(int energyCost, int craftingTime) {
            this.energyCost = energyCost;
            this.craftingTime = craftingTime;
        }
    }

    /**
     * Load configuration from file
     */
    public static void load() {
        Path configPath = Paths.get(CONFIG_FILE);

        // Create default config if not exists
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            // Load category defaults
            if (root.has("defaults")) {
                JsonObject defaults = root.getAsJsonObject("defaults");
                if (defaults.has("ammo")) {
                    JsonObject ammo = defaults.getAsJsonObject("ammo");
                    ammoCost = ammo.has("energyCost") ? ammo.get("energyCost").getAsInt() : DEFAULT_AMMO_COST;
                    ammoTime = ammo.has("craftingTime") ? ammo.get("craftingTime").getAsInt() : DEFAULT_AMMO_TIME;
                }
                if (defaults.has("weapon")) {
                    JsonObject weapon = defaults.getAsJsonObject("weapon");
                    weaponCost = weapon.has("energyCost") ? weapon.get("energyCost").getAsInt() : DEFAULT_WEAPON_COST;
                    weaponTime = weapon.has("craftingTime") ? weapon.get("craftingTime").getAsInt() : DEFAULT_WEAPON_TIME;
                }
            }

            // Load recipe-specific overrides
            if (root.has("recipes")) {
                JsonObject recipes = root.getAsJsonObject("recipes");
                for (String key : recipes.keySet()) {
                    JsonObject recipe = recipes.getAsJsonObject(key);
                    int cost = recipe.has("energyCost") ? recipe.get("energyCost").getAsInt() : DEFAULT_WEAPON_COST;
                    int time = recipe.has("craftingTime") ? recipe.get("craftingTime").getAsInt() : DEFAULT_WEAPON_TIME;
                    RECIPE_COSTS.put(new ResourceLocation(key), new RecipeCost(cost, time));
                }
            }

            System.out.println("[IIC] Loaded Gun Mod workbench energy config: " +
                "Ammo=" + ammoCost + "EU/" + ammoTime + "t, " +
                "Weapon=" + weaponCost + "EU/" + weaponTime + "t, " +
                "Overrides=" + RECIPE_COSTS.size());

        } catch (Exception e) {
            System.err.println("[IIC] Failed to load Gun Mod workbench energy config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create default configuration file
     */
    private static void createDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();

            // Add defaults
            JsonObject defaults = new JsonObject();
            JsonObject ammo = new JsonObject();
            ammo.addProperty("energyCost", DEFAULT_AMMO_COST);
            ammo.addProperty("craftingTime", DEFAULT_AMMO_TIME);
            defaults.add("ammo", ammo);

            JsonObject weapon = new JsonObject();
            weapon.addProperty("energyCost", DEFAULT_WEAPON_COST);
            weapon.addProperty("craftingTime", DEFAULT_WEAPON_TIME);
            defaults.add("weapon", weapon);

            root.add("defaults", defaults);

            // Add example recipe overrides
            JsonObject recipes = new JsonObject();
            JsonObject exampleRecipe = new JsonObject();
            exampleRecipe.addProperty("energyCost", 4096);
            exampleRecipe.addProperty("craftingTime", 480);
            recipes.add("cgm:heavy_rifle", exampleRecipe);

            root.add("recipes", recipes);

            // Add comments as a separate field
            JsonObject comments = new JsonObject();
            comments.addProperty("_comment1", "Gun Mod Workbench Energy Configuration");
            comments.addProperty("_comment2", "energyCost: Energy required in EU (1 EU = 4 FE)");
            comments.addProperty("_comment3", "craftingTime: Time required in ticks (20 ticks = 1 second)");
            comments.addProperty("_comment4", "defaults: Default costs for ammo and weapon categories");
            comments.addProperty("_comment5", "recipes: Per-recipe overrides (use recipe ID from datapack)");
            root.add("_comments", comments);

            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(root, writer);
            }

            System.out.println("[IIC] Created default Gun Mod workbench energy config at: " + configPath);

        } catch (IOException e) {
            System.err.println("[IIC] Failed to create default config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get energy cost for a recipe
     * @param recipeId Recipe resource location
     * @param isAmmo Whether the recipe is for ammo (vs weapon)
     * @return Energy cost in EU
     */
    public static int getEnergyCost(ResourceLocation recipeId, boolean isAmmo) {
        if (RECIPE_COSTS.containsKey(recipeId)) {
            return RECIPE_COSTS.get(recipeId).energyCost;
        }
        return isAmmo ? ammoCost : weaponCost;
    }

    /**
     * Get crafting time for a recipe
     * @param recipeId Recipe resource location
     * @param isAmmo Whether the recipe is for ammo (vs weapon)
     * @return Crafting time in ticks
     */
    public static int getCraftingTime(ResourceLocation recipeId, boolean isAmmo) {
        if (RECIPE_COSTS.containsKey(recipeId)) {
            return RECIPE_COSTS.get(recipeId).craftingTime;
        }
        return isAmmo ? ammoTime : weaponTime;
    }

    /**
     * Check if a recipe has a custom override
     */
    public static boolean hasOverride(ResourceLocation recipeId) {
        return RECIPE_COSTS.containsKey(recipeId);
    }

    /**
     * Get all recipe overrides (for debugging)
     */
    public static Map<ResourceLocation, RecipeCost> getAllOverrides() {
        return new HashMap<>(RECIPE_COSTS);
    }
}
