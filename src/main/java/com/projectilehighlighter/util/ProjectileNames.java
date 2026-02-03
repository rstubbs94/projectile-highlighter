package com.projectilehighlighter.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping projectile IDs to human-readable names.
 * Attempts to load names from RuneLite's ProjectileID at runtime.
 */
@Slf4j
public class ProjectileNames
{
    private static final Map<Integer, String> ID_TO_NAME = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize the mapping by trying to read ProjectileID constants via reflection.
     */
    public static synchronized void initialize()
    {
        if (initialized)
        {
            return;
        }

        // Try to load ProjectileID class dynamically
        try
        {
            Class<?> projectileIdClass = Class.forName("net.runelite.api.ProjectileID");
            for (Field field : projectileIdClass.getDeclaredFields())
            {
                if (field.getType() == int.class && Modifier.isStatic(field.getModifiers()))
                {
                    try
                    {
                        int id = field.getInt(null);
                        String name = formatFieldName(field.getName());
                        ID_TO_NAME.put(id, name);
                    }
                    catch (IllegalAccessException e)
                    {
                        // Skip inaccessible fields
                    }
                }
            }
            log.debug("Loaded {} projectile names from ProjectileID", ID_TO_NAME.size());
        }
        catch (ClassNotFoundException e)
        {
            log.debug("ProjectileID class not found, using ID-only names");
        }
        catch (Exception e)
        {
            log.warn("Failed to load projectile names via reflection", e);
        }

        // Add some commonly known projectiles as fallback
        addKnownProjectiles();

        initialized = true;
    }

    /**
     * Add commonly known projectile names as fallback.
     */
    private static void addKnownProjectiles()
    {
        // Only add if not already loaded via reflection
        ID_TO_NAME.putIfAbsent(53, "Cannonball");
        ID_TO_NAME.putIfAbsent(1443, "Granite Cannonball");

        // Zulrah projectiles
        ID_TO_NAME.putIfAbsent(1044, "Zulrah Snakeling");
        ID_TO_NAME.putIfAbsent(1046, "Zulrah Venom Cloud");

        // Olm projectiles
        ID_TO_NAME.putIfAbsent(1339, "Olm Auto");
        ID_TO_NAME.putIfAbsent(1340, "Olm Mage");
        ID_TO_NAME.putIfAbsent(1341, "Olm Range");
        ID_TO_NAME.putIfAbsent(1347, "Olm Fire Line");
        ID_TO_NAME.putIfAbsent(1357, "Olm Falling Crystal");

        // ToB projectiles
        ID_TO_NAME.putIfAbsent(1580, "Verzik Green Ball");
        ID_TO_NAME.putIfAbsent(1583, "Verzik Purple Crab");

        // Hydra projectiles
        ID_TO_NAME.putIfAbsent(1662, "Hydra Ranged");
        ID_TO_NAME.putIfAbsent(1664, "Hydra Magic");
        ID_TO_NAME.putIfAbsent(1666, "Hydra Poison");
    }

    /**
     * Get the name for a projectile ID.
     * @param projectileId The projectile ID
     * @return The known name, or null if not found
     */
    public static String getName(int projectileId)
    {
        if (!initialized)
        {
            initialize();
        }

        return ID_TO_NAME.get(projectileId);
    }

    /**
     * Get a display name for a projectile ID.
     * @param projectileId The projectile ID
     * @return The known name, or "Projectile #ID" if not found
     */
    public static String getDisplayName(int projectileId)
    {
        String name = getName(projectileId);
        return name != null ? name : "Projectile #" + projectileId;
    }

    /**
     * Check if a projectile ID has a known name.
     */
    public static boolean hasKnownName(int projectileId)
    {
        if (!initialized)
        {
            initialize();
        }
        return ID_TO_NAME.containsKey(projectileId);
    }

    /**
     * Format a field name like "VERZIK_P2_PURPLE" to "Verzik P2 Purple"
     */
    private static String formatFieldName(String fieldName)
    {
        String[] parts = fieldName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts)
        {
            if (result.length() > 0)
            {
                result.append(" ");
            }
            if (part.length() > 0)
            {
                // Check if part is a number or short acronym (like P2)
                if (part.matches("\\d+") || part.matches("[a-z]\\d+"))
                {
                    result.append(part.toUpperCase());
                }
                else
                {
                    result.append(Character.toUpperCase(part.charAt(0)));
                    result.append(part.substring(1));
                }
            }
        }
        return result.toString();
    }
}
