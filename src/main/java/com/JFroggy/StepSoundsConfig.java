package com.JFroggy;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("stepsounds")
public interface StepSoundsConfig extends Config
{
	@ConfigSection(
		name = "Audio Settings",
		description = "General audio settings",
		position = 1
	)
	String audioSettings = "audioSettings";

	@ConfigItem(
		keyName = "masterVolume",
		name = "Master Volume",
		description = "Volume of the step sounds",
		position = 1,
		section = audioSettings
	)
	@Range(max = 100)
	default int masterVolume()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "enableMomentum",
		name = "Momentum Based Sounds",
		description = "Adjust sounds based on movement momentum",
		position = 2,
		section = audioSettings
	)
	default boolean enableMomentum()
	{
		return true;
	}

	@ConfigItem(
		keyName = "variance",
		name = "Volume Variance",
		description = "Random volume variation for each step (0-20%)",
		position = 3,
		section = audioSettings
	)
	@Range(max = 20)
	default int variance()
	{
		return 10;
	}

	@ConfigSection(
		name = "Mapping Settings",
		description = "Toggle mapping systems",
		position = 2
	)
	String mappingSettings = "mappingSettings";

	@ConfigItem(
		keyName = "groundObjectMapping",
		name = "GroundObject Sound Mapping",
		description = "Enable sound mapping based on GroundObjects",
		position = 1,
		section = mappingSettings
	)
	default boolean groundObjectMapping()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tileColorMapping",
		name = "Tile RGB Sound Mapping",
		description = "Enable sound mapping based on Tile RGB color",
		position = 2,
		section = mappingSettings
	)
	default boolean tileColorMapping()
	{
		return true;
	}

	@ConfigSection(
		name = "Debug Settings",
		description = "Debug and development settings",
		position = 99
	)
	String debugSettings = "debugSettings";

	@Alpha
	@ConfigItem(
		keyName = "uncategorizedColor",
		name = "Uncategorized Color",
		description = "Color for objects that haven't been categorized yet",
		position = 0,
		section = debugSettings
	)
	default Color uncategorizedColor()
	{
		return new Color(255, 0, 0, 150);
	}

	@ConfigItem(
		keyName = "showDebugMessages",
		name = "Show Debug Messages",
		description = "Show chat messages for sound events and variables",
		position = 1,
		section = debugSettings
	)
	default boolean showDebugMessages()
	{
		return false;
	}

	@ConfigItem(
		keyName = "categorizationMode",
		name = "Categorization Mode",
		description = "Enable Alt+Click categorization tool",
		position = 2,
		section = debugSettings
	)
	default boolean categorizationMode()
	{
		return false;
	}
}
