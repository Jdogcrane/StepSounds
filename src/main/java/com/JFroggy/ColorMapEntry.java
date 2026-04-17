package com.JFroggy;

import java.awt.Color;
import lombok.Data;

@Data
public class ColorMapEntry
{
	private Color color;
	private String targetCategory;

	public ColorMapEntry(Color color, String targetCategory)
	{
		this.color = color;
		this.targetCategory = targetCategory;
	}
}
