package com.JFroggy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class GroundObjectOverlay extends Overlay
{
	private final Client client;
	private final StepSoundsMain plugin;
	private final StepSoundsConfig config;

	@Inject
	public GroundObjectOverlay(Client client, StepSoundsMain plugin, StepSoundsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.categorizationMode())
		{
			return null;
		}

		Tile[][][] tiles = client.getScene().getTiles();
		int z = client.getPlane();

		for (int x = 0; x < Perspective.SCENE_SIZE; x++)
		{
			for (int y = 0; y < Perspective.SCENE_SIZE; y++)
			{
				Tile tile = tiles[z][x][y];
				if (tile == null) continue;

				renderTileHighlights(graphics, tile);
			}
		}

		return null;
	}

	private void renderTileHighlights(Graphics2D graphics, Tile tile)
	{
		LocalPoint lp = tile.getLocalLocation();
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null) return;

		// 0. Raw RGB Highlight (Debug mode) - Toggled on for all tiles if checked
		if (plugin.isShowAllTilesRgbEnabled())
		{
			Integer tileRgb = plugin.getTileRgb(tile);
			if (tileRgb != null)
			{
				graphics.setColor(new Color(tileRgb));
				graphics.fillPolygon(poly);
				return; // If debug view is on, it overrides everything
			}
		}

		GroundObject groundObject = tile.getGroundObject();
		boolean categorizedGO = false;

		// 1. GroundObject Highlight (Priority 1)
		if (groundObject != null)
		{
			for (PaletteEntry entry : plugin.getPaletteEntries())
			{
				if (entry.getIds().contains(groundObject.getId()))
				{
					categorizedGO = true;
					if (config.groundObjectMapping() && entry.isVisible())
					{
						graphics.setColor(entry.getColor());
						graphics.fillPolygon(poly);
					}
					break;
				}
			}

			// 4. Uncategorized Highlight (Alt Toggle) - Specifically for GroundObjects
			if (!categorizedGO && plugin.isAltPressed())
			{
				graphics.setColor(config.uncategorizedColor());
				graphics.fillPolygon(poly);
				return; 
			}
		}

		// 2. Tile Color Highlight (Spectrum) (Priority 2) - Toggled by Shift key
		if (config.tileColorMapping() && plugin.isShiftPressed())
		{
			Integer tileRgb = plugin.getTileRgb(tile);
			if (tileRgb != null)
			{
				String categoryName = plugin.findClosestColorMapping(tileRgb);
				if (categoryName != null && !categoryName.equals("Default"))
				{
					for (PaletteEntry entry : plugin.getPaletteEntries())
					{
						if (entry.getName().equalsIgnoreCase(categoryName))
						{
							if (entry.isVisible())
							{
								Color c = entry.getColor();
								if (categorizedGO)
								{
									graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 128));
								}
								else
								{
									graphics.setColor(c);
								}
								graphics.fillPolygon(poly);
							}
							break;
						}
					}
				}
			}
		}
	}
}
