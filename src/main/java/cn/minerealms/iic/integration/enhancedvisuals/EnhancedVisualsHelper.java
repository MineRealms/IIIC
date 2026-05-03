package cn.minerealms.iic.integration.enhancedvisuals;

import cn.minerealms.iic.industrial.IndustrialLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * EnhancedVisuals integration helper using reflection to avoid class loading issues.
 *
 * This class uses reflection to call EnhancedVisuals API methods, avoiding direct
 * class references that could cause NoClassDefFoundError at runtime.
 *
 * NOTE: @OnlyIn removed to avoid class loading issues. Runtime side check is used instead.
 */
public class EnhancedVisualsHelper {

    static {
        System.out.println("========================================");
        System.out.println("EnhancedVisualsHelper CLASS LOADED!");
        System.out.println("========================================");
        IndustrialLogger.info("========================================");
        IndustrialLogger.info("EnhancedVisualsHelper CLASS LOADED!");
        IndustrialLogger.info("========================================");
    }

    // Cached reflection objects
    private static Class<?> visualManagerClass;
    private static Class<?> visualRegistryClass;
    private static Class<?> damageHandlerClass;
    private static Class<?> colorClass;
    private static Class<?> intMinMaxClass;
    private static Method addParticlesFadeOutMethod;
    private static Method handlersMethod;
    private static boolean initialized = false;
    private static boolean initFailed = false;

    /**
     * Initialize reflection objects (called once)
     */
    private static void init() {
        if (initialized || initFailed) {
            return;
        }

        try {
            IndustrialLogger.info("[EnhancedVisualsHelper] Initializing reflection...");

            // Load classes
            visualManagerClass = Class.forName("team.creative.enhancedvisuals.client.VisualManager");
            visualRegistryClass = Class.forName("team.creative.enhancedvisuals.common.visual.VisualRegistry");
            damageHandlerClass = Class.forName("team.creative.enhancedvisuals.common.handler.DamageHandler");
            colorClass = Class.forName("team.creative.creativecore.common.util.type.Color");
            intMinMaxClass = Class.forName("team.creative.creativecore.common.config.premade.IntMinMax");

            // Get methods
            handlersMethod = visualRegistryClass.getMethod("handlers");
            addParticlesFadeOutMethod = visualManagerClass.getMethod(
                "addParticlesFadeOut",
                Class.forName("team.creative.enhancedvisuals.api.type.VisualType"),
                Class.forName("team.creative.enhancedvisuals.api.VisualHandler"),
                int.class,
                intMinMaxClass,
                boolean.class,
                colorClass
            );

            initialized = true;
            IndustrialLogger.info("[EnhancedVisualsHelper] Reflection initialized successfully");

        } catch (Throwable e) {
            initFailed = true;
            IndustrialLogger.error("[EnhancedVisualsHelper] Failed to initialize reflection: " + e.getMessage());
            if (IndustrialLogger.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get DamageHandler instance from VisualRegistry
     */
    private static Object getDamageHandler() {
        try {
            Object handlersIterable = handlersMethod.invoke(null);
            for (Object handler : (Iterable<?>) handlersIterable) {
                if (damageHandlerClass.isInstance(handler)) {
                    return handler;
                }
            }
        } catch (Throwable e) {
            IndustrialLogger.error("[EnhancedVisualsHelper] Error getting DamageHandler: " + e.getMessage());
        }
        return null;
    }

    /**
     * Trigger light pollution effects (green particles)
     */
    public static void triggerLightEffects() {
        if (IndustrialLogger.isDebugEnabled()) {
            System.out.println("=== SYSTEM.OUT: triggerLightEffects() ENTRY ===");
            System.err.println("=== SYSTEM.ERR: triggerLightEffects() ENTRY ===");
        }
        IndustrialLogger.debug("[EnhancedVisualsHelper] === triggerLightEffects() ENTRY ===");

        try {
            init();
            if (initFailed) {
                IndustrialLogger.error("[EnhancedVisualsHelper] Cannot trigger effects - initialization failed");
                return;
            }

            IndustrialLogger.debug("[EnhancedVisualsHelper] Step 1: Getting DamageHandler...");
            Object handler = getDamageHandler();
            if (handler == null) {
                IndustrialLogger.error("[EnhancedVisualsHelper] DamageHandler not found!");
                return;
            }
            IndustrialLogger.debug("[EnhancedVisualsHelper] Step 2: DamageHandler found");

            // Get waterDrown field from DamageHandler
            IndustrialLogger.debug("[EnhancedVisualsHelper] Step 3: Getting waterDrown visual type...");
            Field waterDrownField = damageHandlerClass.getField("waterDrown");
            Object visualType = waterDrownField.get(handler);
            IndustrialLogger.debug("[EnhancedVisualsHelper] Step 4: Visual type obtained");

            // Create Color (100, 255, 100, 150) - green
            IndustrialLogger.debug("[EnhancedVisualsHelper] Step 5: Creating green color...");
            Object greenColor = colorClass.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(100, 255, 100, 150);

            // Create IntMinMax(200, 400)
            IndustrialLogger.debug("[EnhancedVisualsHelper] Step 6: Creating duration range...");
            Object duration = intMinMaxClass.getConstructor(int.class, int.class)
                .newInstance(200, 400);

            // Call VisualManager.addParticlesFadeOut
            IndustrialLogger.debug("[EnhancedVisualsHelper] Step 7: Calling addParticlesFadeOut...");
            addParticlesFadeOutMethod.invoke(null, visualType, handler, 15, duration, true, greenColor);

            IndustrialLogger.info("[EnhancedVisualsHelper] Light pollution effects triggered!");

        } catch (Throwable e) {
            IndustrialLogger.error("[EnhancedVisualsHelper] Error triggering light effects: " + e.getMessage());
            if (IndustrialLogger.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Trigger moderate pollution effects (yellow particles)
     */
    public static void triggerModerateEffects() {
        IndustrialLogger.debug("[EnhancedVisualsHelper] === triggerModerateEffects() ENTRY ===");

        try {
            init();
            if (initFailed) return;

            Object handler = getDamageHandler();
            if (handler == null) return;

            Field waterDrownField = damageHandlerClass.getField("waterDrown");
            Object visualType = waterDrownField.get(handler);

            // Yellow color (255, 255, 100, 180)
            Object yellowColor = colorClass.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(255, 255, 100, 180);

            Object duration = intMinMaxClass.getConstructor(int.class, int.class)
                .newInstance(300, 500);

            addParticlesFadeOutMethod.invoke(null, visualType, handler, 20, duration, true, yellowColor);

            IndustrialLogger.info("[EnhancedVisualsHelper] Moderate pollution effects triggered (yellow)");

        } catch (Throwable e) {
            IndustrialLogger.error("[EnhancedVisualsHelper] Error triggering moderate effects: " + e.getMessage());
        }
    }

    /**
     * Trigger heavy pollution effects (orange particles)
     */
    public static void triggerHeavyEffects() {
        IndustrialLogger.debug("[EnhancedVisualsHelper] === triggerHeavyEffects() ENTRY ===");

        try {
            init();
            if (initFailed) return;

            Object handler = getDamageHandler();
            if (handler == null) return;

            Field waterDrownField = damageHandlerClass.getField("waterDrown");
            Object visualType = waterDrownField.get(handler);

            // Orange color (255, 150, 50, 200)
            Object orangeColor = colorClass.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(255, 150, 50, 200);

            Object duration = intMinMaxClass.getConstructor(int.class, int.class)
                .newInstance(400, 600);

            addParticlesFadeOutMethod.invoke(null, visualType, handler, 25, duration, true, orangeColor);

            IndustrialLogger.info("[EnhancedVisualsHelper] Heavy pollution effects triggered (orange)");

        } catch (Throwable e) {
            IndustrialLogger.error("[EnhancedVisualsHelper] Error triggering heavy effects: " + e.getMessage());
        }
    }

    /**
     * Trigger severe pollution effects (red particles)
     */
    public static void triggerSevereEffects() {
        IndustrialLogger.debug("[EnhancedVisualsHelper] === triggerSevereEffects() ENTRY ===");

        try {
            init();
            if (initFailed) return;

            Object handler = getDamageHandler();
            if (handler == null) return;

            Field waterDrownField = damageHandlerClass.getField("waterDrown");
            Object visualType = waterDrownField.get(handler);

            // Red color (255, 50, 50, 220)
            Object redColor = colorClass.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(255, 50, 50, 220);

            Object duration = intMinMaxClass.getConstructor(int.class, int.class)
                .newInstance(500, 800);

            addParticlesFadeOutMethod.invoke(null, visualType, handler, 35, duration, true, redColor);

            IndustrialLogger.info("[EnhancedVisualsHelper] Severe pollution effects triggered (red)");

        } catch (Throwable e) {
            IndustrialLogger.error("[EnhancedVisualsHelper] Error triggering severe effects: " + e.getMessage());
        }
    }

    /**
     * Check if EnhancedVisuals is present
     */
    public static boolean isEnhancedVisualsPresent() {
        try {
            Class.forName("team.creative.enhancedvisuals.client.VisualManager", false, EnhancedVisualsHelper.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
