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
    private EditText etUrl;
    private AppCompatSeekBar seekBarProgress;
    private ImageButton btnPlayPause;
    private TextView tvCurrentTime, tvDuration, tvSource;
    private AppCompatButton btnSetUrl;
    private ProgressBar progressBarLoading;
    private LinearLayout playerContainer;
    private MediaPlayer mediaPlayer;
    private long mediaFileLengthInMilliseconds; // it will contain song duration in milliseconds.
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


        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // listeners
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        seekBarProgress.setOnTouchListener(this);
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

    // Recursive method to update seekBar every 1 second
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

    // To show buffering as a secondary progress in progressBar
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBarProgress.setSecondaryProgress((int) (((float) percent / 100) * mediaFileLengthInMilliseconds));
    }

    // This will be called when Streaming audio gets completed
    @Override
    public void onCompletion(MediaPlayer mp) {
        mediaPlayer.pause();
        seekBarProgress.setProgress(0);
        tvCurrentTime.setText(getTimeToDisplay(0));
        btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    // Method to seek track according to seekBar
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        isSeeking = true;
        tvCurrentTime.setText(getTimeToDisplay(seekBarProgress.getProgress()));
        if (event.getAction() != MotionEvent.ACTION_UP) return false;
        mediaPlayer.seekTo(seekBarProgress.getProgress());
        isSeeking = false;
        return false;
    }

    // simple milli to readable minutes converter
    private String getTimeToDisplay(long milli) {
        return String.format(Locale.ENGLISH, "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milli),
                TimeUnit.MILLISECONDS.toSeconds(milli) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli))
        );
    }

    // onClick for Play/Pause button
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

    // onClick for Stop button
    public void stopOnClick(View view) {
        if (mediaPlayer.isPlaying())
            mediaPlayer.pause();
        seekBarProgress.setProgress(0);
        mediaPlayer.seekTo(0);
        tvCurrentTime.setText(getTimeToDisplay(0));
        btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    // It will be called when MediaPlayer is prepared and ready to play, see mediaPlayer.prepareAsync()
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

    // Releasing resources
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}