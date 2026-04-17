package com.JFroggy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class PalettePanel extends PluginPanel
{
	private final StepSoundsMain plugin;

	private final JPanel container = new JPanel();
	private final JButton addBtn = new JButton("Add Category");
	private final JButton eraseBtn = new JButton("Eraser");
	private final JButton undoBtn = new JButton("Undo");

	@Getter
	private final List<PaletteEntryPanel> entryPanels = new ArrayList<>();

	@Getter
	private PaletteEntryPanel selectedEntryPanel;
	@Getter
	private boolean eraserMode = false;

	public PalettePanel(StepSoundsMain plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel header = new JPanel(new GridLayout(3, 1, 0, 5));
		addBtn.addActionListener(e -> addEntry("New Category", Color.MAGENTA));
		eraseBtn.addActionListener(e -> selectEraser());
		undoBtn.addActionListener(e -> plugin.undo());
		
		header.add(addBtn);
		header.add(eraseBtn);
		header.add(undoBtn);

		container.setLayout(new GridLayout(0, 1, 0, 5));
		JScrollPane scrollPane = new JScrollPane(container);
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

	public void clearEntries()
	{
		entryPanels.clear();
		container.removeAll();
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

	public void selectEntry(PaletteEntryPanel entryPanel)
	{
		eraserMode = false;
		eraseBtn.setBackground(null);

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

		eraserMode = !eraserMode;
		eraseBtn.setBackground(eraserMode ? ColorScheme.PROGRESS_COMPLETE_COLOR : null);
	}

	public void save()
	{
		plugin.savePalette();
	}
}
