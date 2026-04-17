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
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

public class ColorMapEntryPanel extends JPanel
{
	private final ColorMapEntry entry;
	private final PalettePanel parent;

	private final JPanel colorBox = new JPanel();
	private final JTextField targetField = new JTextField();
	private final JButton pickBtn = new JButton("P");
	private final JButton deleteBtn = new JButton("X");

	public ColorMapEntryPanel(ColorMapEntry entry, PalettePanel parent)
	{
		this.entry = entry;
		this.parent = parent;

		setLayout(new BorderLayout(5, 0));
		setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(5, 5, 5, 5)
		));

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

		targetField.setText(entry.getTargetCategory());
		targetField.addActionListener(e -> updateTarget());
		targetField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				updateTarget();
			}
		});

		pickBtn.setPreferredSize(new Dimension(20, 20));
		pickBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
		pickBtn.setToolTipText("Pick color from tile");
		pickBtn.addActionListener(e -> parent.startColorPicking(this));

		deleteBtn.setPreferredSize(new Dimension(20, 20));
		deleteBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
		deleteBtn.addActionListener(e -> {
			int result = JOptionPane.showConfirmDialog(this, "Delete this color mapping?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION)
			{
				parent.removeColorEntry(this);
			}
		});

		JPanel left = new JPanel(new BorderLayout(5, 0));
		left.add(colorBox, BorderLayout.WEST);
		left.add(targetField, BorderLayout.CENTER);

		JPanel right = new JPanel(new BorderLayout(2, 0));
		right.add(pickBtn, BorderLayout.WEST);
		right.add(deleteBtn, BorderLayout.EAST);

		add(left, BorderLayout.CENTER);
		add(right, BorderLayout.EAST);
	}

	private void updateTarget()
	{
		entry.setTargetCategory(targetField.getText());
		parent.save();
	}

	public void updateColor(Color color)
	{
		entry.setColor(color);
		colorBox.setBackground(color);
		revalidate();
		repaint();
	}

	public void setPicking(boolean picking)
	{
		pickBtn.setBackground(picking ? ColorScheme.PROGRESS_COMPLETE_COLOR : null);
	}

	public ColorMapEntry getEntry()
	{
		entry.setTargetCategory(targetField.getText());
		return entry;
	}
}
