package com.JFroggy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Step Sounds",
	description = "Subtle immersive step sounds for your player",
	tags = {"steps", "sounds", "sfx", "qol", "immersion"}
)
public class StepSoundsMain extends Plugin implements KeyListener, MouseListener
{
	private static final String CONFIG_FILE_PATH = "C:/Users/J/Desktop/Files/Games/Minecraft Civ Experiment/CivlabsPublic/StepSounds/GroundObjectsConfig/config.txt";

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

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Gson gson;

	private WorldPoint lastPosition;
	private int runTicks = 0;
	private boolean wasMoving = false;
	private float lastFinalVolume = 0.5f;
	private float lastPitch = 1.0f;

	@Getter
	private boolean altPressed = false;

	private PalettePanel palettePanel;
	private NavigationButton navButton;

	private final Stack<String> undoStack = new Stack<>();

	@Override
	protected void startUp() throws Exception
	{
		audioManager.init();
		overlayManager.add(groundObjectOverlay);
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(this);

		palettePanel = new PalettePanel(this);
		loadPalette();

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Step Sounds Palette")
			.icon(icon)
			.priority(6)
			.panel(palettePanel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.info("Step Sounds started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		audioManager.shutDown();
		overlayManager.remove(groundObjectOverlay);
		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(this);
		clientToolbar.removeNavigation(navButton);
		log.info("Step Sounds stopped!");
	}

	private String serializeCurrentState()
	{
		List<PaletteEntryState> state = new ArrayList<>();
		for (PaletteEntryPanel panel : palettePanel.getEntryPanels())
		{
			PaletteEntry entry = panel.getEntry();
			state.add(new PaletteEntryState(entry.getName(), entry.getColor().getRGB(), entry.isVisible(), new HashSet<>(entry.getIds())));
		}
		return gson.toJson(state);
	}

	private static class PaletteEntryState
	{
		String name;
		int color;
		boolean visible;
		Set<Integer> ids;

		PaletteEntryState(String name, int color, boolean visible, Set<Integer> ids)
		{
			this.name = name;
			this.color = color;
			this.visible = visible;
			this.ids = ids;
		}
	}

	public void savePalette()
	{
		savePalette(true);
	}

	public void savePalette(boolean pushUndo)
	{
		StringBuilder sb = new StringBuilder();
		for (PaletteEntryPanel panel : palettePanel.getEntryPanels())
		{
			PaletteEntry entry = panel.getEntry();
			sb.append("# ").append(entry.getName())
				.append(";").append(entry.getColor().getRGB())
				.append(";").append(entry.isVisible())
				.append("\n");

			sb.append(entry.getName()).append(" IDS:\n");
			List<Integer> sortedIds = entry.getIds().stream().sorted().collect(Collectors.toList());
			for (Integer id : sortedIds)
			{
				sb.append("    ").append(id).append("\n");
			}
			sb.append("\n");
		}

		try
		{
			File file = new File(CONFIG_FILE_PATH);
			file.getParentFile().mkdirs();
			Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.error("Could not save config to " + CONFIG_FILE_PATH, e);
		}
	}

	public void undo()
	{
		if (undoStack.isEmpty()) return;

		String stateJson = undoStack.pop();
		List<PaletteEntryState> states = gson.fromJson(stateJson, new TypeToken<List<PaletteEntryState>>(){}.getType());

		palettePanel.clearEntries();
		for (PaletteEntryState state : states)
		{
			palettePanel.addEntry(state.name, new Color(state.color, true));
			PaletteEntry newEntry = palettePanel.getEntryPanels().get(palettePanel.getEntryPanels().size() - 1).getEntry();
			newEntry.setVisible(state.visible);
			newEntry.setIds(state.ids);
		}
		savePalette(false);
	}

	private void loadPalette()
	{
		File file = new File(CONFIG_FILE_PATH);
		if (!file.exists()) return;

		try
		{
			List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			PaletteEntry currentEntry = null;

			for (String line : lines)
			{
				String trimmed = line.trim();
				if (trimmed.isEmpty()) continue;

				if (line.startsWith("# "))
				{
					String meta = line.substring(2);
					String[] parts = meta.split(";");
					if (parts.length >= 3)
					{
						String name = parts[0];
						Color color = new Color(Integer.parseInt(parts[1]), true);
						boolean visible = Boolean.parseBoolean(parts[2]);
						
						palettePanel.addEntry(name, color);
						currentEntry = palettePanel.getEntryPanels().get(palettePanel.getEntryPanels().size() - 1).getEntry();
						currentEntry.setVisible(visible);
					}
				}
				else if (line.endsWith(" IDS:") || line.endsWith(" IDS::"))
				{
				}
				else if (line.startsWith("    ") && currentEntry != null)
				{
					try
					{
						currentEntry.getIds().add(Integer.parseInt(trimmed));
					}
					catch (NumberFormatException ignored) {}
				}
			}
		}
		catch (IOException e)
		{
			log.error("Could not load config from " + CONFIG_FILE_PATH, e);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ALT)
		{
			altPressed = !altPressed;
		}
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON2) // Middle Mouse Button
		{
			Tile tile = client.getSelectedSceneTile();
			if (tile != null && tile.getGroundObject() != null)
			{
				int id = tile.getGroundObject().getId();
				
				undoStack.push(serializeCurrentState());
				if (undoStack.size() > 50) undoStack.remove(0);

				if (palettePanel.isEraserMode())
				{
					removeFromAllCategories(id);
					e.consume();
				}
				else if (palettePanel.getSelectedEntryPanel() != null)
				{
					removeFromAllCategories(id);
					palettePanel.getSelectedEntryPanel().getEntry().getIds().add(id);
					savePalette(false);
					e.consume();
				}
				else
				{
					undoStack.pop();
				}
			}
		}
		return e;
	}

	private void removeFromAllCategories(int id)
	{
		for (PaletteEntryPanel panel : palettePanel.getEntryPanels())
		{
			panel.getEntry().getIds().remove(id);
		}
	}

	public List<PaletteEntry> getPaletteEntries()
	{
		return palettePanel.getEntryPanels().stream()
			.map(PaletteEntryPanel::getEntry)
			.collect(Collectors.toList());
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
		//run
		{
			audioManager.playStepSoundDelayed(finalVolume, pitch, 50);
			audioManager.playStepSoundDelayed(finalVolume, pitch, 350);
		}
		//walk
		else
		{
			audioManager.playStepSoundDelayed(finalVolume, pitch, 500);
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
