package com.JFroggy;

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
		if (!config.categorizationMode() || !plugin.isAltPressed())
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

				GroundObject groundObject = tile.getGroundObject();
				if (groundObject != null)
				{
					renderGroundObject(graphics, groundObject);
				}
			}
		}

		return null;
	}

	private void renderGroundObject(Graphics2D graphics, GroundObject groundObject)
	{
		LocalPoint lp = groundObject.getLocalLocation();
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);

		if (poly != null)
		{
			GroundType type = plugin.getGroundType(groundObject.getId());
			graphics.setColor(type.getHighlightColor());
			graphics.fillPolygon(poly);
		}
	}
}
