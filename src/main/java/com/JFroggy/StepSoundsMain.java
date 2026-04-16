package com.JFroggy;

import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

@Slf4j
@PluginDescriptor(
	name = "Step Sounds",
	description = "Subtle immersive step sounds for your player",
	tags = {"steps", "sounds", "sfx", "qol", "immersion"}
)
public class StepSoundsMain extends Plugin implements KeyListener, MouseListener
{
	@Inject
	private Client client;

	@Inject
	private StepSoundsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private AudioManager audioManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GroundObjectOverlay groundObjectOverlay;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	private WorldPoint lastPosition;
	private int runTicks = 0;
	private boolean wasMoving = false;
	private float lastFinalVolume = 0.5f;
	private float lastPitch = 1.0f;

	@Getter
	private boolean altPressed = false;

	@Override
	protected void startUp() throws Exception
	{
		audioManager.init();
		overlayManager.add(groundObjectOverlay);
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(this);
		log.info("Step Sounds started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		audioManager.shutDown();
		overlayManager.remove(groundObjectOverlay);
		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(this);
		log.info("Step Sounds stopped!");
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ALT)
		{
			altPressed = !altPressed;
			if (config.showDebugMessages())
			{
				sendDebugMessage("Categorization mode: " + (altPressed ? "ENABLED" : "DISABLED"));
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		if (altPressed && config.categorizationMode() && e.getButton() == MouseEvent.BUTTON1)
		{
			handleCategorizationClick(e);
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e) { return e; }

	@Override
	public MouseEvent mouseReleased(MouseEvent e) { return e; }

	@Override
	public MouseEvent mouseEntered(MouseEvent e) { return e; }

	@Override
	public MouseEvent mouseExited(MouseEvent e) { return e; }

	@Override
	public MouseEvent mouseDragged(MouseEvent e) { return e; }

	@Override
	public MouseEvent mouseMoved(MouseEvent e) { return e; }

	private void handleCategorizationClick(MouseEvent e)
	{
		Tile tile = client.getSelectedSceneTile();
		if (tile == null) return;

		GroundObject groundObject = tile.getGroundObject();
		if (groundObject == null) return;

		int id = groundObject.getId();
		sendDebugMessage("Right-click Ground Object ID: " + id + " to categorize.");
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!altPressed || !config.categorizationMode()) return;

		Tile tile = client.getSelectedSceneTile();
		if (tile == null || tile.getGroundObject() == null) return;

		int id = tile.getGroundObject().getId();

		for (GroundType type : GroundType.values())
		{
			if (type == GroundType.UNCATEGORIZED)
			{
				if (isCategorized(id))
				{
					client.createMenuEntry(-1)
						.setOption(ColorUtil.prependColorTag("Uncategorize", type.getHighlightColor()))
						.setTarget(id + "")
						.setType(MenuAction.RUNELITE)
						.onClick(e -> unCategorize(id));
				}
				continue;
			}

			client.createMenuEntry(-1)
				.setOption(ColorUtil.prependColorTag("Categorize as " + type.name(), type.getHighlightColor()))
				.setTarget(id + "")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> categorize(id, type));
		}
	}

	private void unCategorize(int id)
	{
		unCategorize(id, true);
	}

	private void unCategorize(int id, boolean notify)
	{
		removeFromCategory(id, "stoneIds", config.stoneIds());
		removeFromCategory(id, "grassIds", config.grassIds());
		removeFromCategory(id, "dirtIds", config.dirtIds());
		removeFromCategory(id, "woodIds", config.woodIds());
		removeFromCategory(id, "fabricIds", config.fabricIds());
		if (notify)
		{
			sendDebugMessage("Uncategorized ID " + id);
		}
	}

	private void removeFromCategory(int id, String key, String currentIds)
	{
		Set<Integer> ids = parseIds(currentIds);
		if (ids.remove(id))
		{
			String newIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
			configManager.setConfiguration("stepsounds", key, newIds);
		}
	}

	private void categorize(int id, GroundType type)
	{
		unCategorize(id, false);

		String key = "";
		String currentIds = "";
		switch (type)
		{
			case STONE: key = "stoneIds"; currentIds = config.stoneIds(); break;
			case GRASS: key = "grassIds"; currentIds = config.grassIds(); break;
			case DIRT: key = "dirtIds"; currentIds = config.dirtIds(); break;
			case WOOD: key = "woodIds"; currentIds = config.woodIds(); break;
			case FABRIC: key = "fabricIds"; currentIds = config.fabricIds(); break;
		}

		if (key.isEmpty()) return;

		Set<Integer> ids = parseIds(currentIds);
		ids.add(id);
		String newIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
		configManager.setConfiguration("stepsounds", key, newIds);
		sendDebugMessage("Categorized ID " + id + " as " + type.name());
	}

	private boolean isCategorized(int id)
	{
		return getGroundType(id) != GroundType.UNCATEGORIZED;
	}

	public GroundType getGroundType(int id)
	{
		if (parseIds(config.stoneIds()).contains(id)) return GroundType.STONE;
		if (parseIds(config.grassIds()).contains(id)) return GroundType.GRASS;
		if (parseIds(config.dirtIds()).contains(id)) return GroundType.DIRT;
		if (parseIds(config.woodIds()).contains(id)) return GroundType.WOOD;
		if (parseIds(config.fabricIds()).contains(id)) return GroundType.FABRIC;
		return GroundType.UNCATEGORIZED;
	}

	private Set<Integer> parseIds(String csv)
	{
		if (csv == null || csv.isEmpty()) return new HashSet<>();
		return Arrays.stream(csv.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(Integer::parseInt)
			.collect(Collectors.toSet());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null) return;

		WorldPoint currentPosition = (localPlayer.getWorldLocation());

		if (lastPosition != null)
		{
			int distance = currentPosition.distanceTo(lastPosition);
			if (distance > 0)
			{
				handleMovement(distance, currentPosition);
				wasMoving = true;
			}
			else
			{
				if (wasMoving)
				{
					audioManager.playStepSoundDelayed(lastFinalVolume * 0.9f, lastPitch * 0.95f, 0);
					wasMoving = false;
				}
				runTicks = Math.max(0, runTicks - 1);
			}
		}
		lastPosition = currentPosition;
	}

	private void handleMovement(int distance, WorldPoint location)
	{
		if (distance > 1) runTicks = Math.min(runTicks + 2, 20);
		else runTicks = Math.min(runTicks + 1, 10);

		float momentum = config.enableMomentum() ? (float)runTicks / 20f : 0f;
		float pitch = 0.9f + (momentum * 0.25f);
		float baseVolume = (config.masterVolume() / 100f);
		float volume = baseVolume * (0.8f + (momentum * 0.2f));

		String context = detectContext(location);
		float contextMod = 1.0f;
		if (context.equals("Decoration")) contextMod = 1.1f;
		else if (context.equals("Object")) contextMod = 1.2f;

		float finalVolume = volume * contextMod;
		
		lastFinalVolume = finalVolume;
		lastPitch = pitch;

		if (distance > 1)
		{
			audioManager.playStepSoundDelayed(finalVolume, pitch, 50);
			audioManager.playStepSoundDelayed(finalVolume, pitch, 350);
		}
		else
		{
			audioManager.playStepSoundDelayed(finalVolume * 0.9f, pitch * 0.95f, 100);
			audioManager.playStepSoundDelayed(finalVolume, pitch, 400);
		}
	}

	private String detectContext(WorldPoint location)
	{
		Tile[][][] tiles = client.getScene().getTiles();
		int z = client.getPlane();
		int x = location.getX() - client.getBaseX();
		int y = location.getY() - client.getBaseY();

		if (x >= 0 && x < 104 && y >= 0 && y < 104)
		{
			Tile tile = tiles[z][x][y];
			if (tile != null)
			{
				if (tile.getDecorativeObject() != null) return "Decoration";
				if (tile.getGroundObject() != null) return "GroundObject";
			}
		}
		return "Default";
	}

	private void sendDebugMessage(String msg)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[StepSounds Debug] " + msg, null);
	}

	@Provides
	StepSoundsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StepSoundsConfig.class);
	}
}
