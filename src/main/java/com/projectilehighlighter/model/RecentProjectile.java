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

	public String getSourceDisplay()
	{
		return sourceActorName != null ? sourceActorName : "Unknown Source";
	}
}
