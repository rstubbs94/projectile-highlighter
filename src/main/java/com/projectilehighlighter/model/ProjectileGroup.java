package com.projectilehighlighter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a group of projectiles with shared enable/disable state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectileGroup
{
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String name;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private List<ProjectileEntry> entries = new ArrayList<>();

    public void addEntry(ProjectileEntry entry)
    {
        if (entries == null)
        {
            entries = new ArrayList<>();
        }
        entries.add(entry);
    }

    public void removeEntry(ProjectileEntry entry)
    {
        if (entries != null)
        {
            entries.remove(entry);
        }
    }

    public void removeEntryById(int projectileId)
    {
        if (entries != null)
        {
            entries.removeIf(e -> e.getProjectileId() == projectileId);
        }
    }

    public ProjectileEntry findEntryById(int projectileId)
    {
        if (entries == null)
        {
            return null;
        }
        return entries.stream()
            .filter(e -> e.getProjectileId() == projectileId)
            .findFirst()
            .orElse(null);
    }

    public int getEntryCount()
    {
        return entries != null ? entries.size() : 0;
    }
}
