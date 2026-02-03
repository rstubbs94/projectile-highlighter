package com.projectilehighlighter.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.projectilehighlighter.model.ProjectileEntry;
import com.projectilehighlighter.model.ProjectileGroup;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles persistence of projectile groups to JSON.
 * Storage location: ~/.runelite/projectile-highlighter/groups.json
 */
@Slf4j
public class GroupStorage
{
    private static final String FOLDER_NAME = "projectile-highlighter";
    private static final String GROUPS_FILE_NAME = "groups.json";

    private final Gson gson;
    private final File groupsFile;
    private List<ProjectileGroup> groups;

    private Consumer<List<ProjectileGroup>> onGroupsChangedCallback;

    public GroupStorage(Gson gson)
    {
        this.gson = gson.newBuilder()
            .setPrettyPrinting()
            .create();

        File folder = new File(RuneLite.RUNELITE_DIR, FOLDER_NAME);
        if (!folder.exists())
        {
            folder.mkdirs();
        }

        this.groupsFile = new File(folder, GROUPS_FILE_NAME);
        this.groups = new ArrayList<>();

        loadGroups();
    }

    public void setOnGroupsChangedCallback(Consumer<List<ProjectileGroup>> callback)
    {
        this.onGroupsChangedCallback = callback;
    }

    private void notifyGroupsChanged()
    {
        if (onGroupsChangedCallback != null)
        {
            onGroupsChangedCallback.accept(new ArrayList<>(groups));
        }
    }

    public void loadGroups()
    {
        if (!groupsFile.exists())
        {
            log.debug("No existing groups file found at {}", groupsFile.getPath());
            groups = new ArrayList<>();
            return;
        }

        try (FileReader reader = new FileReader(groupsFile))
        {
            Type listType = new TypeToken<ArrayList<ProjectileGroup>>(){}.getType();
            List<ProjectileGroup> loaded = gson.fromJson(reader, listType);

            if (loaded != null)
            {
                groups = loaded;
                log.info("Loaded {} projectile groups from file", groups.size());
            }
            else
            {
                groups = new ArrayList<>();
            }
        }
        catch (IOException e)
        {
            log.error("Failed to load groups from file", e);
            groups = new ArrayList<>();
        }
        catch (Exception e)
        {
            log.error("Failed to parse groups file", e);
            groups = new ArrayList<>();
        }
    }

    public void saveGroups()
    {
        try (FileWriter writer = new FileWriter(groupsFile))
        {
            gson.toJson(groups, writer);
            log.debug("Saved {} projectile groups to file", groups.size());
        }
        catch (IOException e)
        {
            log.error("Failed to save groups to file", e);
        }
    }

    public List<ProjectileGroup> getGroups()
    {
        return new ArrayList<>(groups);
    }

    public List<ProjectileGroup> getEnabledGroups()
    {
        List<ProjectileGroup> enabled = new ArrayList<>();
        for (ProjectileGroup group : groups)
        {
            if (group.isEnabled())
            {
                enabled.add(group);
            }
        }
        return enabled;
    }

    public void addGroup(ProjectileGroup group)
    {
        groups.add(0, group);
        saveGroups();
        notifyGroupsChanged();
    }

    public void updateGroup(ProjectileGroup group)
    {
        for (int i = 0; i < groups.size(); i++)
        {
            if (groups.get(i).getId().equals(group.getId()))
            {
                groups.set(i, group);
                break;
            }
        }
        saveGroups();
        notifyGroupsChanged();
    }

    public void deleteGroup(ProjectileGroup group)
    {
        groups.removeIf(g -> g.getId().equals(group.getId()));
        saveGroups();
        notifyGroupsChanged();
    }

    public void deleteGroupById(String groupId)
    {
        groups.removeIf(g -> g.getId().equals(groupId));
        saveGroups();
        notifyGroupsChanged();
    }

    public void renameGroup(ProjectileGroup group, String newName)
    {
        group.setName(newName);
        updateGroup(group);
    }

    public void toggleGroupEnabled(ProjectileGroup group)
    {
        group.setEnabled(!group.isEnabled());
        updateGroup(group);
    }

    public ProjectileGroup findGroupById(String groupId)
    {
        return groups.stream()
            .filter(g -> g.getId().equals(groupId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find which group contains a projectile entry with the given ID.
     */
    public ProjectileGroup findGroupContainingProjectile(int projectileId)
    {
        for (ProjectileGroup group : groups)
        {
            if (group.findEntryById(projectileId) != null)
            {
                return group;
            }
        }
        return null;
    }

    /**
     * Get the ProjectileEntry for a given projectile ID from any enabled group.
     */
    public ProjectileEntry getEnabledEntry(int projectileId)
    {
        for (ProjectileGroup group : getEnabledGroups())
        {
            ProjectileEntry entry = group.findEntryById(projectileId);
            if (entry != null)
            {
                return entry;
            }
        }
        return null;
    }

    /**
     * Check if any enabled group contains the given projectile ID.
     */
    public boolean isProjectileEnabled(int projectileId)
    {
        return getEnabledEntry(projectileId) != null;
    }
}
