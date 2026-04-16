package com.JFroggy;

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
		name = "Categorization Data",
		description = "Stored IDs for ground types",
		position = 10
	)
	String categorizationData = "categorizationData";

	@ConfigItem(
		keyName = "stoneIds",
		name = "Stone IDs",
		description = "Comma separated list of stone IDs",
		position = 1,
		section = categorizationData
	)
	default String stoneIds() { return ""; }

	@ConfigItem(
		keyName = "grassIds",
		name = "Grass IDs",
		description = "Comma separated list of grass IDs",
		position = 2,
		section = categorizationData
	)
	default String grassIds() { return ""; }

	@ConfigItem(
		keyName = "dirtIds",
		name = "Dirt IDs",
		description = "Comma separated list of dirt IDs",
		position = 3,
		section = categorizationData
	)
	default String dirtIds() { return ""; }

	@ConfigItem(
		keyName = "woodIds",
		name = "Wood IDs",
		description = "Comma separated list of wood IDs",
		position = 4,
		section = categorizationData
	)
	default String woodIds() { return ""; }

	@ConfigItem(
		keyName = "fabricIds",
		name = "Fabric IDs",
		description = "Comma separated list of fabric IDs",
		position = 5,
		section = categorizationData
	)
	default String fabricIds() { return ""; }

	@ConfigSection(
		name = "Debug Settings",
		description = "Debug and development settings",
		position = 99
	)
	String debugSettings = "debugSettings";

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
