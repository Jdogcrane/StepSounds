package com.JFroggy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class PalettePanel extends PluginPanel
{
	private final StepSoundsMain plugin;

	private final JPanel container = new JPanel();
	private final JPanel colorContainer = new JPanel();

	private final JButton addBtn = new JButton("Add Category");
	private final JButton eraseBtn = new JButton("Eraser");
	private final JButton undoBtn = new JButton("Undo");
	
	private final JCheckBox groundObjectToggle = new JCheckBox("Ground Object Mapping", true);
	private final JCheckBox tileColorToggle = new JCheckBox("Tile Color Mapping", true);
	private final JCheckBox showAllRgbToggle = new JCheckBox("Show all tiles RGB", false);
	private final JCheckBox showDebugMessagesToggle = new JCheckBox("Show Debug Messages", false);
	private final JCheckBox categorizationModeToggle = new JCheckBox("Show Debug Overlay", false); // New checkbox for categorization mode

	private final JButton addColorBtn = new JButton("Add Color Mapping");

	@Getter
	private final List<PaletteEntryPanel> entryPanels = new ArrayList<>();
	@Getter
	private final List<ColorMapEntryPanel> colorPanels = new ArrayList<>();

	@Getter
	private PaletteEntryPanel selectedEntryPanel;
	@Getter
	private ColorMapEntryPanel pickingColorPanel;
	@Getter
	private boolean eraserMode = false;

	public PalettePanel(StepSoundsMain plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

		groundObjectToggle.addActionListener(e -> plugin.updateMappingToggles());
		tileColorToggle.addActionListener(e -> plugin.updateMappingToggles());
		showDebugMessagesToggle.addActionListener(e -> plugin.updateMappingToggles());
		categorizationModeToggle.addActionListener(e -> plugin.updateMappingToggles()); // Add action listener for new toggle

		header.add(groundObjectToggle);
		header.add(tileColorToggle);
		header.add(showAllRgbToggle);
		header.add(showDebugMessagesToggle);
		header.add(categorizationModeToggle); // Add new checkbox to header
		header.add(new JSeparator());
		
		JPanel buttonGroup = new JPanel(new GridLayout(3, 1, 0, 5));
		addBtn.addActionListener(e -> addEntry("New Category", Color.MAGENTA));
		eraseBtn.addActionListener(e -> selectEraser());
		undoBtn.addActionListener(e -> plugin.undo());
		
		buttonGroup.add(addBtn);
		buttonGroup.add(eraseBtn);
		buttonGroup.add(undoBtn);
		header.add(buttonGroup);

		container.setLayout(new GridLayout(0, 1, 0, 5));
		colorContainer.setLayout(new GridLayout(0, 1, 0, 5));

		JPanel mainScrollContainer = new JPanel();
		mainScrollContainer.setLayout(new BoxLayout(mainScrollContainer, BoxLayout.Y_AXIS));
		
		mainScrollContainer.add(new JLabel("Categories:"));
		mainScrollContainer.add(container);
		mainScrollContainer.add(new JSeparator());
		
		JPanel colorHeader = new JPanel(new BorderLayout());
		colorHeader.add(new JLabel("Color Mappings:"), BorderLayout.WEST);
		addColorBtn.addActionListener(e -> addColorEntry(Color.GREEN, ""));
		colorHeader.add(addColorBtn, BorderLayout.EAST);
		
		mainScrollContainer.add(colorHeader);
		mainScrollContainer.add(colorContainer);

		JScrollPane scrollPane = new JScrollPane(mainScrollContainer);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		add(header, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void addEntry(String name, Color color)
	{
		PaletteEntry entry = new PaletteEntry(name, color);
		PaletteEntryPanel entryPanel = new PaletteEntryPanel(entry, this);
		entryPanels.add(entryPanel);
		container.add(entryPanel);
		revalidate();
		repaint();
		save();
	}

	public void addColorEntry(Color color, String target)
	{
		ColorMapEntry entry = new ColorMapEntry(color, target);
		ColorMapEntryPanel panel = new ColorMapEntryPanel(entry, this);
		colorPanels.add(panel);
		colorContainer.add(panel);
		revalidate();
		repaint();
		save();
	}

	public void clearEntries()
	{
		entryPanels.clear();
		container.removeAll();
		colorPanels.clear();
		colorContainer.removeAll();
		revalidate();
		repaint();
	}

	public void removeEntry(PaletteEntryPanel entryPanel)
	{
		if (selectedEntryPanel == entryPanel)
		{
			selectedEntryPanel = null;
		}
		entryPanels.remove(entryPanel);
		container.remove(entryPanel);
		revalidate();
		repaint();
		save();
	}

	public void removeColorEntry(ColorMapEntryPanel panel)
	{
		if (pickingColorPanel == panel) pickingColorPanel = null;
		colorPanels.remove(panel);
		colorContainer.remove(panel);
		revalidate();
		repaint();
		save();
	}

	public void selectEntry(PaletteEntryPanel entryPanel)
	{
		eraserMode = false;
		eraseBtn.setBackground(null);
		stopColorPicking();

		if (selectedEntryPanel == entryPanel)
		{
			selectedEntryPanel.setSelected(false);
			selectedEntryPanel = null;
		}
		else
		{
			if (selectedEntryPanel != null)
			{
				selectedEntryPanel.setSelected(false);
			}
			selectedEntryPanel = entryPanel;
			selectedEntryPanel.setSelected(true);
		}
	}

	private void selectEraser()
	{
		if (selectedEntryPanel != null)
		{
			selectedEntryPanel.setSelected(false);
			selectedEntryPanel = null;
		}
		stopColorPicking();

		eraserMode = !eraserMode;
		eraseBtn.setBackground(eraserMode ? ColorScheme.PROGRESS_COMPLETE_COLOR : null);
	}

	public void startColorPicking(ColorMapEntryPanel panel)
	{
		if (selectedEntryPanel != null)
		{
			selectedEntryPanel.setSelected(false);
			selectedEntryPanel = null;
		}
		eraserMode = false;
		eraseBtn.setBackground(null);

		if (pickingColorPanel == panel)
		{
			stopColorPicking();
		}
		else
		{
			stopColorPicking();
			pickingColorPanel = panel;
			pickingColorPanel.setPicking(true);
		}
	}

	public void stopColorPicking()
	{
		if (pickingColorPanel != null)
		{
			pickingColorPanel.setPicking(false);
		}
		pickingColorPanel = null;
	}

	public void updateToggles(boolean go, boolean tc, boolean showDebug, boolean categorizationMode) // Modified to include categorizationMode
	{
		groundObjectToggle.setSelected(go);
		tileColorToggle.setSelected(tc);
		showDebugMessagesToggle.setSelected(showDebug);
		categorizationModeToggle.setSelected(categorizationMode); // Set initial state for categorization mode
	}

	public boolean isGroundObjectMappingEnabled() { return groundObjectToggle.isSelected(); }
	public boolean isTileColorMappingEnabled() { return tileColorToggle.isSelected(); }
	public boolean isShowAllTilesRgbEnabled() { return showAllRgbToggle.isSelected(); }
	public boolean isShowDebugMessagesEnabled() { return showDebugMessagesToggle.isSelected(); }
	public boolean isCategorizationModeEnabled() { return categorizationModeToggle.isSelected(); } // New getter

	public void save()
	{
		plugin.savePalette();
	}
}
