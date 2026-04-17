package com.JFroggy;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;

@Slf4j
@Singleton
public class AudioManager
{
	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private StepSoundsConfig config;

	@Inject
	private Provider<StepSoundsMain> pluginProvider;

	@Inject
	private ClientThread clientThread;

	private static final File BASE_DIR = new File(RuneLite.RUNELITE_DIR, "stepsounds");
	private final Map<String, List<byte[]>> soundCache = new HashMap<>();
	private AudioFormat soundFormat;
	private final Random random = new Random();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public void init()
	{
		reloadSounds();
	}

	public void reloadSounds()
	{
		soundCache.clear();
		if (!BASE_DIR.exists())
		{
			BASE_DIR.mkdirs();
		}

		File[] categoryDirs = BASE_DIR.listFiles(File::isDirectory);
		if (categoryDirs != null)
		{
			for (File dir : categoryDirs)
			{
				// Ignore "data" or other internal folders if they exist, 
				// but user wants stepsounds/<categoryname>
				loadCategory(dir.getName().toLowerCase(), dir);
			}
		}
		
		if (!soundCache.containsKey("generic") || soundCache.get("generic").isEmpty())
		{
			loadGenericFromResources();
		}
		
		log.info("AudioManager loaded " + soundCache.size() + " sound categories.");
	}

	private void loadCategory(String name, File dir)
	{
		File[] files = dir.listFiles((d, f) -> f.toLowerCase().endsWith(".wav"));
		if (files == null || files.length == 0) return;

		List<byte[]> samples = new ArrayList<>();
		for (File f : files)
		{
			byte[] data = loadSample(f);
			if (data != null) samples.add(data);
		}
		if (!samples.isEmpty()) soundCache.put(name, samples);
	}

	private void loadGenericFromResources()
	{
		List<byte[]> samples = new ArrayList<>();
		byte[] s1 = loadSampleFromResource("/step.wav");
		byte[] s2 = loadSampleFromResource("/step2.wav");
		if (s1 != null) samples.add(s1);
		if (s2 != null) samples.add(s2);
		if (!samples.isEmpty()) soundCache.put("generic", samples);
	}

	private byte[] loadSample(File file)
	{
		try (InputStream is = new FileInputStream(file))
		{
			return readAudioData(is);
		}
		catch (Exception e)
		{
			log.error("Failed to load sample: " + file.getPath(), e);
			return null;
		}
	}

	private byte[] loadSampleFromResource(String path)
	{
		try (InputStream is = getClass().getResourceAsStream(path))
		{
			if (is == null) return null;
			return readAudioData(is);
		}
		catch (Exception e)
		{
			log.error("Failed to load resource sample: " + path, e);
			return null;
		}
	}

	private byte[] readAudioData(InputStream is) throws Exception
	{
		AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
		soundFormat = ais.getFormat();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = ais.read(buffer)) != -1)
		{
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}

	public void playStepSoundDelayed(float volume, float pitch, long delayMs)
	{
		scheduler.schedule(() -> playStepSound(volume, pitch), delayMs, TimeUnit.MILLISECONDS);
	}

	public void playStepSound(float volume, float pitch)
	{
		// Category detection must happen on client thread
		clientThread.invokeLater(() -> {
			String category = pluginProvider.get().detectCurrentCategory();
			String catKey = (category == null) ? "generic" : category.toLowerCase();
			
			List<byte[]> samples = soundCache.get(catKey);
			boolean usingFallback = false;

			if (samples == null || samples.isEmpty())
			{
				samples = soundCache.get("generic");
				usingFallback = true;
				if (config.showDebugMessages())
				{
					pluginProvider.get().sendDebugMessage("Category '" + catKey + "' not found or empty. Using generic.");
				}
			}
			else if (config.showDebugMessages())
			{
				pluginProvider.get().sendDebugMessage("Playing sound for category: " + catKey);
			}

			if (samples == null || samples.isEmpty() || soundFormat == null)
			{
				if (config.showDebugMessages())
				{
					pluginProvider.get().sendDebugMessage("FAILED to find any sounds (including generic).");
				}
				return;
			}

			final List<byte[]> finalSamples = samples;
			final String finalCat = catKey;
			
			// Play the sound (this part can be off-thread, but AudioPlayer is usually thread-safe)
			byte[] originalBytes = finalSamples.get(random.nextInt(finalSamples.size()));

			try {
				float vVar = config.variance() / 100f;
				float finalVolume = volume * (1.0f + (random.nextFloat() * vVar * 2 - vVar));
				finalVolume = Math.max(0.0001f, Math.min(1.0f, finalVolume));

				float pVar = 0.06f;
				float finalPitch = pitch * (1.0f + (random.nextFloat() * pVar * 2 - pVar));

				float gainDb = (float) (Math.log10(finalVolume) * 20.0);
				byte[] processedBytes = resample(originalBytes, soundFormat, finalPitch);

				ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
				AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(processedBytes), soundFormat, processedBytes.length / soundFormat.getFrameSize()), AudioFileFormat.Type.WAVE, wavOut);
				
				audioPlayer.play(new ByteArrayInputStream(wavOut.toByteArray()), gainDb);
			} catch (Exception e) {
				log.error("Error processing audio for category: " + finalCat, e);
			}
		});
	}

	private byte[] resample(byte[] input, AudioFormat format, float pitch) {
		if (Math.abs(pitch - 1.0f) < 0.01f) return input;
		if (format.getSampleSizeInBits() != 16) return input;

		short[] samples = bytesToShorts(input, format.isBigEndian());
		int newLength = (int) (samples.length / pitch);
		short[] output = new short[newLength];

		for (int i = 0; i < newLength; i++) {
			float index = i * pitch;
			int low = (int) Math.floor(index);
			int high = Math.min(low + 1, samples.length - 1);
			float weight = index - low;
			if (low < samples.length) {
				output[i] = (short) ((1 - weight) * samples[low] + weight * samples[high]);
			}
		}
		return shortsToBytes(output, format.isBigEndian());
	}

	private short[] bytesToShorts(byte[] bytes, boolean bigEndian) {
		short[] shorts = new short[bytes.length / 2];
		ByteBuffer.wrap(bytes).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
		return shorts;
	}

	private byte[] shortsToBytes(short[] shorts, boolean bigEndian) {
		byte[] bytes = new byte[shorts.length * 2];
		ByteBuffer.wrap(bytes).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
		return bytes;
	}

	public void shutDown() {
		scheduler.shutdown();
	}
}
