package com.JFroggy;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum GroundType
{
	STONE(new Color(128, 128, 128, 100)), // Gray
	GRASS(new Color(0, 255, 0, 100)),     // Green
	DIRT(new Color(93, 38, 0, 100)),    // Brown
	WOOD(new Color(255, 80, 0, 100)),    // Sienna
	FABRIC(new Color(255, 0, 255, 100)),  // Magenta
	UNCATEGORIZED(new Color(255, 0, 0, 100)); // Red

	private final Color highlightColor;
}
