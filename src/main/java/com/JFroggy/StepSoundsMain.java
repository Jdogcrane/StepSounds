package com.JFroggy;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Step Sounds",
	description = "Subtle immersive step sounds for your player",
	tags = {"steps", "sounds", "sfx", "qol", "immersion"}
)
public class StepSoundsMain extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private StepSoundsConfig config;

	@Inject
	private AudioManager audioManager;

	private WorldPoint lastPosition;
	private int runTicks = 0;
	private boolean wasMoving = false;
	private float lastFinalVolume = 0.5f;
	private float lastPitch = 1.0f;

	@Override
	protected void startUp() throws Exception
	{
		audioManager.init();
		log.info("Step Sounds started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		audioManager.shutDown();
		log.info("Step Sounds stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null) return;

		WorldPoint currentPosition = localPlayer.getWorldLocation();

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
					// Re-added the stop sound with the requested parameters
					audioManager.playStepSoundDelayed(lastFinalVolume * 0.9f, lastPitch * 0.95f, 100);
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
		
		// Store these for the stop sound
		lastFinalVolume = finalVolume;
		lastPitch = pitch;

		if (distance > 1) // Running
		{
			// Play two steps with 0.3s (300ms) interval
			audioManager.playStepSoundDelayed(finalVolume, pitch, 50);
			audioManager.playStepSoundDelayed(finalVolume, pitch, 350);
		}
		else // Walking
		{
			//  500ms between each step
//			audioManager.playStepSoundDelayed(finalVolume, pitch, 50);
			audioManager.playStepSoundDelayed(finalVolume, pitch, 450);
		}

		if (config.showDebugMessages())
		{
			sendDebugMessage(String.format("Mode: %s | Mom: %.2f", distance > 1 ? "Run" : "Walk", momentum));
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
				if (tile.getGameObjects() != null) {
					for (GameObject obj : tile.getGameObjects()) {
						if (obj != null) return "Object";
					}
				}
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
