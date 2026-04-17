package com.JFroggy;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class PaletteEntry
{
	private String name;
	private Color color;
	private boolean visible = true;
	private transient Set<Integer> ids = new HashSet<>();

	public PaletteEntry(String name, Color color)
	{
		this.name = name;
		this.color = color;
	}
}
