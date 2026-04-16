package com.JFroggy;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;

@Slf4j
@Singleton
public class AudioManager
{
	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private StepSoundsConfig config;

	private byte[] step1Bytes;
	private byte[] step2Bytes;
	private AudioFormat stepFormat;
	private final Random random = new Random();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public void init()
	{
		step1Bytes = loadSample("/step.wav");
		step2Bytes = loadSample("/step2.wav");
	}

	private byte[] loadSample(String path)
	{
		try (InputStream is = getClass().getResourceAsStream(path))
		{
			if (is == null) return null;
			AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
			stepFormat = ais.getFormat();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int read;
			while ((read = ais.read(buffer)) != -1)
			{
				baos.write(buffer, 0, read);
			}
			return baos.toByteArray();
		}
		catch (Exception e)
		{
			log.error("Failed to load sample: " + path, e);
			return null;
		}
	}

	public void playStepSoundDelayed(float volume, float pitch, long delayMs)
	{
		scheduler.schedule(() -> playStepSound(volume, pitch), delayMs, TimeUnit.MILLISECONDS);
	}

	public void playStepSound(float volume, float pitch)
	{
		// Randomly pick between the two samples
		byte[] originalBytes = random.nextBoolean() ? step1Bytes : step2Bytes;
		if (originalBytes == null || stepFormat == null) return;

		try {
			// Apply volume variance
			float vVar = config.variance() / 100f;
			float finalVolume = volume * (1.0f + (random.nextFloat() * vVar * 2 - vVar));
			finalVolume = Math.max(0.0001f, Math.min(1.0f, finalVolume));

			// High Pitch variance as requested (12% range)
			float pVar = 0.8f;
			float finalPitch = pitch * (1.0f + (random.nextFloat() * pVar * 2 - pVar));

			float gainDb = (float) (Math.log10(finalVolume) * 20.0);
			byte[] processedBytes = resample(originalBytes, stepFormat, finalPitch);

			ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
			AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(processedBytes), stepFormat, processedBytes.length / stepFormat.getFrameSize()), AudioFileFormat.Type.WAVE, wavOut);
			
			audioPlayer.play(new ByteArrayInputStream(wavOut.toByteArray()), gainDb);
		} catch (Exception e) {
			log.error("Error processing audio", e);
		}
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
