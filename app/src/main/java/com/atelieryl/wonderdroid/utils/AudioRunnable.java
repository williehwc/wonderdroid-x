
package com.atelieryl.wonderdroid.utils;

import com.atelieryl.wonderdroid.WonderSwan;

import android.media.AudioTrack;

public class AudioRunnable implements Runnable {

	private AudioTrack audio;
	
	public AudioRunnable(AudioTrack audio) {
		this.audio = audio;
	}

	@Override
	public void run() {
		audio.write(WonderSwan.audiobuffer, 0, /*WonderSwan.prevSamples * 2 + */WonderSwan.samples * 2);
	}
	
}