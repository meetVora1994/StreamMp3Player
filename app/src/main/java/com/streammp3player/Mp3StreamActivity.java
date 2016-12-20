package com.streammp3player;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Mp3StreamActivity extends AppCompatActivity implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, View.OnTouchListener, MediaPlayer.OnPreparedListener {

    private final Handler handler = new Handler();
    EditText etUrl;
    AppCompatSeekBar seekBarProgress;
    ImageButton btnPlayPause;
    TextView tvCurrentTime, tvDuration, tvSource;
    AppCompatButton btnSetUrl;
    ProgressBar progressBarLoading;
    private LinearLayout playerContainer;
    private MediaPlayer mediaPlayer;
    private long mediaFileLengthInMilliseconds; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
    private boolean isSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_stream);

        etUrl = (EditText) findViewById(R.id.etUrl);
        seekBarProgress = (AppCompatSeekBar) findViewById(R.id.seekBar);
        tvCurrentTime = (TextView) findViewById(R.id.tvCurrentTime);
        tvDuration = (TextView) findViewById(R.id.tvDuration);
        btnPlayPause = (ImageButton) findViewById(R.id.btnPlayPause);
        tvSource = (TextView) findViewById(R.id.tvSource);
        playerContainer = (LinearLayout) findViewById(R.id.playerContainer);
        btnSetUrl = (AppCompatButton) findViewById(R.id.btnSetUrl);
        progressBarLoading = (ProgressBar) findViewById(R.id.progressBarLoading);

        playerContainer.setVisibility(View.GONE);

        seekBarProgress.setOnTouchListener(this);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);

        mediaPlayer.setOnPreparedListener(this);

        btnSetUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnSetUrl.getText().toString().equalsIgnoreCase(getString(R.string.set_stream_url))) {
                    if (TextUtils.isEmpty(etUrl.getText().toString())) {
                        Toast.makeText(Mp3StreamActivity.this, "Please enter Url", Toast.LENGTH_SHORT).show();
                        etUrl.requestFocus();
                        return;
                    }
                    progressBarLoading.setVisibility(View.VISIBLE);
                    try {
                        mediaPlayer.setDataSource(etUrl.getText().toString()); // setup song from https://www.hrupin.com/wp-content/uploads/mp3/testsong_20_sec.mp3 URL to mediaplayer data source
                        mediaPlayer.prepareAsync();
                    } catch (Exception e) {
                        progressBarLoading.setVisibility(View.GONE);
                        Toast.makeText(Mp3StreamActivity.this, "Failed to set this URL", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    btnSetUrl.setText(R.string.set_stream_url);
                    etUrl.setVisibility(View.VISIBLE);
                    playerContainer.setVisibility(View.GONE);
                    mediaPlayer.reset();
                    seekBarProgress.setProgress(0);
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
        });

    }

    /**
     * Method which updates the SeekBar primary progress by current song playing position
     */
    private void primarySeekBarProgressUpdater() {
        if (mediaPlayer.isPlaying()) {
            if (!isSeeking) {
                seekBarProgress.setProgress(mediaPlayer.getCurrentPosition());
                tvCurrentTime.setText(getTimeToDisplay(seekBarProgress.getProgress()));
            }
            Runnable notification = new Runnable() {
                public void run() {
                    primarySeekBarProgressUpdater();
                }
            };
            handler.postDelayed(notification, 1000);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBarProgress.setSecondaryProgress((int) (((float) percent / 100) * mediaFileLengthInMilliseconds));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mediaPlayer.pause();
        seekBarProgress.setProgress(0);
        tvCurrentTime.setText(getTimeToDisplay(0));
        btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        isSeeking = true;
        /** Seekbar onTouch event handler. Method which seeks MediaPlayer to seekBar primary progress position*/
        tvCurrentTime.setText(getTimeToDisplay(seekBarProgress.getProgress()));
        if (event.getAction() != MotionEvent.ACTION_UP) return false;
        mediaPlayer.seekTo(seekBarProgress.getProgress());
        isSeeking = false;
        return false;
    }

    private String getTimeToDisplay(long milli) {
        return String.format(Locale.ENGLISH, "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milli),
                TimeUnit.MILLISECONDS.toSeconds(milli) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli))
        );
    }

    public void playPauseOnClick(View view) {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
        primarySeekBarProgressUpdater();
    }

    public void stopOnClick(View view) {
        if (mediaPlayer.isPlaying())
            mediaPlayer.pause();
        seekBarProgress.setProgress(0);
        mediaPlayer.seekTo(0);
        tvCurrentTime.setText(getTimeToDisplay(0));
        btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        progressBarLoading.setVisibility(View.GONE);
        tvSource.setText(etUrl.getText().toString());
        btnSetUrl.setText(R.string.change_stream_url);
        etUrl.setVisibility(View.GONE);
        playerContainer.setVisibility(View.VISIBLE);
        seekBarProgress.setMax(mp.getDuration());
        mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
        tvDuration.setText(getTimeToDisplay(mediaFileLengthInMilliseconds));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}