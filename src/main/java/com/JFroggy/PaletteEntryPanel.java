package com.JFroggy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

public class PaletteEntryPanel extends JPanel
{
	private final PaletteEntry entry;
	private final PalettePanel parent;

	private final JTextField nameField = new JTextField();
	private final JPanel colorBox = new JPanel();
	private final JCheckBox visibleBox = new JCheckBox();
	private final JButton selectBtn = new JButton("S");
	private final JButton deleteBtn = new JButton("X");

	public PaletteEntryPanel(PaletteEntry entry, PalettePanel parent)
	{
		this.entry = entry;
		this.parent = parent;

		setLayout(new BorderLayout(5, 0));
		setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(5, 5, 5, 5)
		));

		nameField.setText(entry.getName());
		nameField.addActionListener(e -> updateName());
		nameField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				updateName();
			}
		});

		colorBox.setBackground(entry.getColor());
		colorBox.setPreferredSize(new Dimension(20, 20));
		colorBox.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				Color color = JColorChooser.showDialog(parent, "Choose Color", entry.getColor());
				if (color != null)
				{
					entry.setColor(color);
					colorBox.setBackground(color);
					parent.save();
				}
			}
		});

		visibleBox.setSelected(entry.isVisible());
		visibleBox.addActionListener(e -> {
			entry.setVisible(visibleBox.isSelected());
			parent.save();
		});

		selectBtn.setPreferredSize(new Dimension(20, 20));
		selectBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
		selectBtn.addActionListener(e -> parent.selectEntry(this));

		deleteBtn.setPreferredSize(new Dimension(20, 20));
		deleteBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
		deleteBtn.addActionListener(e -> {
			int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this category?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION)
			{
				parent.removeEntry(this);
			}
		});

		JPanel left = new JPanel(new BorderLayout(5, 0));
		left.add(colorBox, BorderLayout.WEST);
		left.add(nameField, BorderLayout.CENTER);

		JPanel right = new JPanel(new BorderLayout(5, 0));
		right.add(visibleBox, BorderLayout.WEST);
		right.add(selectBtn, BorderLayout.CENTER);
		right.add(deleteBtn, BorderLayout.EAST);

		add(left, BorderLayout.CENTER);
		add(right, BorderLayout.EAST);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getSource() == nameField || e.getSource() == colorBox || e.getSource() == visibleBox || e.getSource() == deleteBtn || e.getSource() == selectBtn) return;
				parent.selectEntry(PaletteEntryPanel.this);
			}
		});
	}

	private void updateName()
	{
		entry.setName(nameField.getText());
		parent.save();
	}

	public void setSelected(boolean selected)
	{
		if (selected)
		{
			setBorder(new CompoundBorder(
				BorderFactory.createLineBorder(ColorScheme.PROGRESS_COMPLETE_COLOR, 1),
				new EmptyBorder(4, 4, 4, 4)
			));
		}
		else
		{
			setBorder(new CompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(5, 5, 5, 5)
			));
		}
	}

	public PaletteEntry getEntry()
	{
		entry.setName(nameField.getText());
		return entry;
	}
}
