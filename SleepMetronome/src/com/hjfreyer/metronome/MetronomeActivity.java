// Copyright 2011 Hunter Freyer (yt@hjfreyer.com)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hjfreyer.metronome;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

public class MetronomeActivity extends Activity {

  private static final int CLICK_BUFFER_SIZE = 4096;
  private static final int SAMPLE_RATE = 22050;

  static final String TAG = "SleepMetronome";

  private EditText durationEdit;
  private EditText startEdit;
  private EditText endEdit;

  private short[] click;
  private int clickLen;

  private Thread thread = null;
  private AudioTrack track;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    durationEdit = (EditText) findViewById(R.id.editDuration);
    startEdit = (EditText) findViewById(R.id.editStart);
    endEdit = (EditText) findViewById(R.id.editEnd);

    int bufferSize =
      AudioTrack.getMinBufferSize(SAMPLE_RATE,
                                  AudioFormat.CHANNEL_OUT_MONO,
                                  AudioFormat.ENCODING_PCM_16BIT);
    track = new AudioTrack(AudioManager.STREAM_MUSIC,
                           SAMPLE_RATE,
                           AudioFormat.CHANNEL_OUT_MONO,
                           AudioFormat.ENCODING_PCM_16BIT,
                           bufferSize,
                           AudioTrack.MODE_STREAM);

    try {
      initClick();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
  }

  public void toggleMetro(View v) throws IOException, InterruptedException {
    ToggleButton toggle = (ToggleButton) v;
    if (!toggle.isChecked()) {
      track.stop();
      track.flush();
      thread.join();
      thread = null;
      return;
    }

    double duration = Double.parseDouble(durationEdit.getText().toString());
    double startHz =
      Double.parseDouble(startEdit.getText().toString()) / 60;
    double endHz = Double.parseDouble(endEdit.getText().toString()) / 60;

    track.play();
    TickTrackGenerator t = new TickTrackGenerator(startHz, endHz, duration,
                                                  track, click, clickLen);
    thread = new Thread(t);
    thread.start();
  }

  public void initClick() throws IOException {
    InputStream raw = getResources().openRawResource(R.raw.click);

    click = new short[CLICK_BUFFER_SIZE];
    clickLen = 0;
    while (true) {
      int a = raw.read();
      if (a == -1) {
        break;
      }

      int b = raw.read();
      if (b == -1) {
        throw new EOFException("Found EOF half way through short.");
      }

      // Little endian byte pair to short.
      click[clickLen++] = (short)((b << 8) | (a & 0xff));
    }
  }
}