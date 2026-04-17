package com.JFroggy;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
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
	private static final File CONFIG_DIR = new File(RuneLite.RUNELITE_DIR, "stepsounds");
	private static final File CONFIG_FILE = new File(CONFIG_DIR, "config.txt");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

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

	@Getter
	private boolean shiftPressed = false;

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
		
		palettePanel.updateToggles(config.groundObjectMapping(), config.tileColorMapping(), config.showDebugMessages(), config.categorizationMode());

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

	public void updateMappingToggles()
	{
		// Force re-reading current state to ensure accuracy
		boolean newGroundObjectMapping = palettePanel.isGroundObjectMappingEnabled();
		boolean newTileColorMapping = palettePanel.isTileColorMappingEnabled();
		boolean newShowDebugMessages = palettePanel.isShowDebugMessagesEnabled();
		boolean newCategorizationMode = palettePanel.isCategorizationModeEnabled();

		configManager.setConfiguration("stepsounds", "groundObjectMapping", newGroundObjectMapping);
		configManager.setConfiguration("stepsounds", "tileColorMapping", newTileColorMapping);
		configManager.setConfiguration("stepsounds", "showDebugMessages", newShowDebugMessages);
		configManager.setConfiguration("stepsounds", "categorizationMode", newCategorizationMode);
	}

	public String detectCurrentCategory()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null) return "generic";

		WorldPoint location = localPlayer.getWorldLocation();
		String cat = detectCategoryAt(location);
		
		if (cat.equals("Default"))
		{
			Map<String, Integer> counts = new HashMap<>();
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					if (dx == 0 && dy == 0) continue;
					String neighborCat = detectCategoryAt(new WorldPoint(location.getX() + dx, location.getY() + dy, location.getPlane()));
					if (!neighborCat.equals("Default"))
					{
						counts.put(neighborCat, counts.getOrDefault(neighborCat, 0) + 1);
					}
				}
			}
			
			if (!counts.isEmpty())
			{
				return Collections.max(counts.entrySet(), Map.Entry.comparingByValue()).getKey();
			}
			return "generic";
		}
		
		return cat;
	}

	private String detectCategoryAt(WorldPoint location)
	{
		Tile[][][] tiles = client.getScene().getTiles();
		int z = location.getPlane();
		int x = location.getX() - client.getBaseX();
		int y = location.getY() - client.getBaseY();

		if (x >= 0 && x < 104 && y >= 0 && y < 104)
		{
			Tile tile = tiles[z][x][y];
			if (tile != null)
			{
				// GroundObject sound mapping - purely config-driven
				if (config.groundObjectMapping() && tile.getGroundObject() != null)
				{
					int id = tile.getGroundObject().getId();
					for (PaletteEntry entry : getPaletteEntries())
					{
						if (entry.getIds().contains(id)) return entry.getName();
					}
				}
				
				// Tile RGB sound mapping - purely config-driven
				if (config.tileColorMapping())
				{
					Integer tileRgb = getTileRgb(tile);
					if (tileRgb != null) return findClosestColorMapping(tileRgb);
				}
			}
		}
		return "Default";
	}

	private String serializeCurrentState()
	{
		StringBuilder sb = new StringBuilder();
		for (PaletteEntryPanel panel : palettePanel.getEntryPanels())
		{
			PaletteEntry entry = panel.getEntry();
			sb.append("#CAT ").append(entry.getName()).append(";").append(entry.getColor().getRGB()).append(";").append(entry.isVisible()).append("\n");
			sb.append(entry.getName()).append(" IDS:\n");
			List<Integer> sortedIds = entry.getIds().stream().sorted().collect(Collectors.toList());
			for (Integer id : sortedIds) sb.append("    ").append(id).append("\n");
			sb.append("\n");
		}
		for (ColorMapEntryPanel panel : palettePanel.getColorPanels())
		{
			ColorMapEntry entry = panel.getEntry();
			sb.append("#CLR ").append(entry.getColor().getRGB()).append(";").append(entry.getTargetCategory()).append("\n");
		}
		return sb.toString();
	}

	public void savePalette()
	{
		savePalette(true);
	}

	public void savePalette(boolean pushUndo)
	{
		if (pushUndo)
		{
			undoStack.push(serializeCurrentState());
			if (undoStack.size() > 50) undoStack.remove(0);
		}

		String state = serializeCurrentState();

		try
		{
			CONFIG_DIR.mkdirs();
			Files.write(CONFIG_FILE.toPath(), state.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.error("Could not save config to " + CONFIG_FILE, e);
		}
	}

	public void undo()
	{
		if (undoStack.isEmpty()) return;

		String lastState = undoStack.pop();
		parseState(Arrays.asList(lastState.split("\n")));
		savePalette(false);
	}

	private void loadPalette()
	{
		List<String> lines = new ArrayList<>();

		try
		{
			if (CONFIG_FILE.exists())
			{
				lines = Files.readAllLines(CONFIG_FILE.toPath(), StandardCharsets.UTF_8);
			}
			else
			{
				try (InputStream is = getClass().getResourceAsStream("/config.txt"))
				{
					if (is != null)
					{
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
						{
							lines = reader.lines().collect(Collectors.toList());
						}
					}
				}
			}
			
			if (!lines.isEmpty())
			{
				parseState(lines);
			}
		}
		catch (IOException e)
		{
			log.error("Could not load config", e);
		}
	}

	private void parseState(List<String> lines)
	{
		palettePanel.clearEntries();
		PaletteEntry currentEntry = null;

		for (String line : lines)
		{
			String trimmed = line.trim();
			if (trimmed.isEmpty()) continue;

			if (line.startsWith("#CAT ") || (line.startsWith("# ") && line.contains(";")))
			{
				String meta = line.startsWith("#CAT ") ? line.substring(5) : line.substring(2);
				String[] parts = meta.split(";");
				if (parts.length >= 2)
				{
					String name = parts[0];
					Color color = new Color(Integer.parseInt(parts[1]), true);
					boolean visible = parts.length < 3 || Boolean.parseBoolean(parts[2]);
					
					palettePanel.addEntry(name, color);
					currentEntry = palettePanel.getEntryPanels().get(palettePanel.getEntryPanels().size() - 1).getEntry();
					currentEntry.setVisible(visible);
				}
			}
			else if (line.startsWith("#CLR "))
			{
				String meta = line.substring(5);
				String[] parts = meta.split(";");
				if (parts.length >= 2)
				{
					Color color = new Color(Integer.parseInt(parts[0]), true);
					palettePanel.addColorEntry(color, parts[1]);
				}
			}
			else if (line.endsWith(" IDS:") || line.endsWith(" IDS::"))
			{
				String name = line.substring(0, line.lastIndexOf(" IDS")).trim();
				currentEntry = palettePanel.getEntryPanels().stream()
					.map(PaletteEntryPanel::getEntry)
					.filter(e -> e.getName().equalsIgnoreCase(name))
					.findFirst().orElse(null);
			}
			else if (line.startsWith("    ") && currentEntry != null)
			{
				try { currentEntry.getIds().add(Integer.parseInt(trimmed)); } catch (Exception ignored) {}
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ALT) altPressed = true;
		else if (e.getKeyCode() == KeyEvent.VK_SHIFT) shiftPressed = true;
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ALT) altPressed = false;
		else if (e.getKeyCode() == KeyEvent.VK_SHIFT) shiftPressed = false;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON2) // Middle Mouse
		{
			Tile tile = client.getSelectedSceneTile();
			if (tile == null) return e;

			if (palettePanel.getPickingColorPanel() != null)
			{
				Integer rgb = getTileRgb(tile);
				if (rgb != null)
				{
					palettePanel.getPickingColorPanel().updateColor(new Color(rgb));
					palettePanel.stopColorPicking();
					savePalette();
				}
				e.consume();
				return e;
			}

			GroundObject groundObject = tile.getGroundObject();
			if (groundObject != null)
			{
				int id = groundObject.getId();
				undoStack.push(serializeCurrentState());
				if (undoStack.size() > 50) undoStack.remove(0);

				if (palettePanel.isEraserMode())
				{
					removeFromAllCategories(id);
					savePalette(false);
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

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		return e;
	}

	public Integer getTileRgb(Tile tile)
	{
		SceneTilePaint paint = tile.getSceneTilePaint();
		return (paint != null) ? paint.getRBG() : null;
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
		return palettePanel.getEntryPanels().stream().map(PaletteEntryPanel::getEntry).collect(Collectors.toList());
	}
	
	public List<ColorMapEntry> getColorMappings()
	{
		return palettePanel.getColorPanels().stream().map(ColorMapEntryPanel::getEntry).collect(Collectors.toList());
	}

	public String findClosestColorMapping(int rgb)
	{
		Color tileColor = new Color(rgb);
		String bestMatch = "Default";
		double minDiff = Double.MAX_VALUE;

		for (ColorMapEntry entry : getColorMappings())
		{
			double diff = getColorDifference(tileColor, entry.getColor());
			if (diff < minDiff)
			{
				minDiff = diff;
				bestMatch = entry.getTargetCategory();
			}
		}
		return bestMatch;
	}

	private double getColorDifference(Color c1, Color c2)
	{
		long rmean = ( (long)c1.getRed() + (long)c2.getRed() ) / 2;
		long r = (long)c1.getRed() - (long)c2.getRed();
		long g = (long)c1.getGreen() - (long)c2.getGreen();
		long b = (long)c1.getBlue() - (long)c2.getBlue();
		return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
	}

	public boolean isShowAllTilesRgbEnabled()
	{
		return palettePanel != null && palettePanel.isShowAllTilesRgbEnabled();
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
					audioManager.playStepSoundDelayed(lastFinalVolume * 0.9f, lastPitch * 0.95f, 25);
					wasMoving = false;
				}
				runTicks = Math.max(0, runTicks - 1);
			}
		}
		lastPosition = currentPosition;
	}

	private void handleMovement(int distance, WorldPoint location)
	{
		boolean isRunning = distance > 1;
		float basePitch = (float)config.walkPitch() / 100f;
		float runIncrease = (float)config.runPitchIncrease() / 100f;
		float pitch = isRunning ? (basePitch + runIncrease) : basePitch;
		float baseVolume = (config.masterVolume() / 100f);

		lastFinalVolume = baseVolume;
		lastPitch = pitch;

		if (isRunning)
		{
			audioManager.playStepSoundDelayed(lastFinalVolume, pitch, 0);
			audioManager.playStepSoundDelayed(lastFinalVolume, pitch, 300);
		}
		else
		{
			audioManager.playStepSoundDelayed(lastFinalVolume, pitch, 500);
		}
	}

	public void sendDebugMessage(String msg)
	{
		if (config.showDebugMessages())
		{
			clientThread.invokeLater(() -> {
				client.addChatMessage(ChatMessageType.CONSOLE, "", "[StepSounds] " + msg, null);
			});
		}
	}

	@Provides StepSoundsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StepSoundsConfig.class);
	}
}
