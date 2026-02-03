package com.projectilehighlighter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a recently seen projectile for the UI list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentProjectile
{
    private int projectileId;
    private String sourceActorName;
    private String targetActorName;
    private long timestamp;

    public String getTimeSinceSeen()
    {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        long seconds = diff / 1000;

        if (seconds < 60)
        {
            return seconds + "s ago";
        }
        long minutes = seconds / 60;
        if (minutes < 60)
        {
            return minutes + "m ago";
        }
        return (minutes / 60) + "h ago";
    }

    public String getActorInfo()
    {
        String source = sourceActorName != null ? sourceActorName : "Unknown";
        String target = targetActorName != null ? targetActorName : "Unknown";
        return source + " -> " + target;
    }
}
