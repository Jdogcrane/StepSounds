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
}
