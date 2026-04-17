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
		return 20;
	}

	@ConfigItem(
		keyName = "volumeVariance",
		name = "Volume Variance",
		description = "Random volume variation for each step (0-20%)",
		position = 2,
		section = audioSettings
	)
	@Range(max = 20)
	default int volumeVariance()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "walkPitch",
		name = "Walk Pitch",
		description = "Base pitch for walking sounds",
		position = 3,
		section = audioSettings
	)
	@Range(min = 50, max = 150) // Representing 0.5 to 1.5 as 50 to 150
	default int walkPitch()
	{
		return 100; // Default to 1.0f
	}

	@ConfigItem(
		keyName = "runPitchIncrease",
		name = "Run Pitch Increase",
		description = "How much higher the pitch is when running (0-50%)",
		position = 4,
		section = audioSettings
	)
	@Range(max = 50)
	default int runPitchIncrease()
	{
		return 45; // Default to 10% increase
	}

	@ConfigItem(
		keyName = "pitchVariance",
		name = "Pitch Variance",
		description = "Random pitch variation for each step (0-20%)",
		position = 5,
		section = audioSettings
	)
	@Range(max = 20)
	default int pitchVariance()
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

	@ConfigItem(
		keyName = "combineMappings",
		name = "Combine Mapping Systems",
		description = "Future: Blend sounds if both GroundObject and RGB mappings are present",
		position = 3,
		section = mappingSettings
	)
	default boolean combineMappings()
	{
		return false;
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
