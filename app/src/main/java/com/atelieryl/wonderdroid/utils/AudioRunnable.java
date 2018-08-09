
package com.atelieryl.wonderdroid.utils;

import com.atelieryl.wonderdroid.WonderSwan;

import android.media.AudioTrack;
import android.util.Log;

public class AudioThread extends Thread {

	private AudioTrack audio;
	private boolean play = false;
	private boolean running = true;
	
	public AudioThread(AudioTrack audio) {
		this.audio = audio;
	}

	public void setPlay() {
		play = true;
	}

	public void clearRunning() {
		running = false;
	}

	@Override
	public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
		while (running) {
			if (play) {
				audio.write(WonderSwan.audiobuffer, 0, WonderSwan.prevSamples * 2 + WonderSwan.samples * 2);
			}
			play = false;
		}
		synchronized (this) {
			notifyAll();
		}
	}
	
}