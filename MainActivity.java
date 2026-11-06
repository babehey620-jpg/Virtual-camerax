package com.example.virtualcamerastudio;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSource;
import com.google.android.exoplayer2.ui.PlayerView;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(7, 18, 55);
    private final int panel = Color.rgb(20, 43, 111);
    private final int cyan = Color.rgb(0, 190, 239);
    private FrameLayout previewFrame;
    private ImageView imageView;
    private PlayerView playerView;
    private EditText rtmpInput;
    private TextView fileLabel;
    private ExoPlayer player;
    private Uri selectedUri;
    private boolean isVideo = false;
    private boolean loop = true;
    private boolean correction = true;
    private boolean floating = false;
    private float zoom = 1f, dx = 0f, dy = 0f, downX, downY;
    private int rotation = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildUi();
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setTextSize(14);
        b.setBackground(rounded(cyan, 28));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(16, 12, 16, 12);
        l.setBackground(rounded(panel, 22));
        return l;
    }

    private void addSpace(LinearLayout l, int h) {
        View s = new View(this);
        l.addView(s, new LinearLayout.LayoutParams(1, h));
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(16, 20, 16, 24);
        scroll.addView(page);

        TextView title = text("VIRTUAL CAMERA STUDIO", 20, Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(null, 1);
        page.addView(title);
        TextView subtitle = text("Media preview & camera testing", 12, 0xFFB9C9F4);
        page.addView(subtitle);
        addSpace(page, 18);

        LinearLayout source = column();
        source.addView(text("●  SOURCE", 13, 0xFF83CFFF));
        addSpace(source, 8);
        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton mediaMode = new RadioButton(this); mediaMode.setId(View.generateViewId()); mediaMode.setText("Image / Video");
        mediaMode.setTextColor(Color.WHITE); mediaMode.setChecked(true);
        RadioButton streamMode = new RadioButton(this); streamMode.setId(View.generateViewId()); streamMode.setText("RTMP Stream");
        streamMode.setTextColor(Color.WHITE);
        modes.addView(mediaMode); modes.addView(streamMode);
        source.addView(modes);
        fileLabel = text("No media selected", 13, 0xFFDDE8FF);
        source.addView(fileLabel);
        addSpace(source, 8);
        source.addView(button("SELECT IMAGE / VIDEO", this::pickMedia));
        addSpace(source, 6);
        rtmpInput = new EditText(this);
        rtmpInput.setSingleLine(true);
        rtmpInput.setHint("rtmp://server/app/stream");
        rtmpInput.setHintTextColor(0xFF9BAEDC);
        rtmpInput.setTextColor(Color.WHITE);
        rtmpInput.setBackground(rounded(0xFF10265F, 14));
        rtmpInput.setPadding(12, 8, 12, 8);
        source.addView(rtmpInput);
        addSpace(source, 6);
        source.addView(button("START RTMP PREVIEW", this::startRtmp));
        page.addView(source);
        addSpace(page, 14);

        LinearLayout previewCard = column();
        previewCard.addView(text("◉  LIVE PREVIEW", 13, 0xFF83CFFF));
        addSpace(previewCard, 8);
        previewFrame = new FrameLayout(this);
        previewFrame.setBackground(rounded(Color.BLACK, 16));
        imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        previewFrame.addView(imageView, new FrameLayout.LayoutParams(-1, -1));
        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setVisibility(View.GONE);
        previewFrame.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        TextView placeholder = text("Choose an image or video to preview", 14, 0xFFB7C7E8);
        placeholder.setGravity(Gravity.CENTER);
        previewFrame.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
        previewFrame.setTag(placeholder);
        previewCard.addView(previewFrame, new LinearLayout.LayoutParams(-1, 230));
        addSpace(previewCard, 8);
        previewCard.addView(button("RESTORE PREVIEW", this::restorePreview));
        page.addView(previewCard);
        addSpace(page, 14);

        LinearLayout controls = column();
        controls.addView(text("⚙  CONTROLS", 13, 0xFF83CFFF));
        addSpace(controls, 8);
        controls.addView(text("Zoom", 13, Color.WHITE));
        SeekBar zoomBar = new SeekBar(this); zoomBar.setMax(200); zoomBar.setProgress(0);
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { zoom = 1f + p / 100f; applyTransform(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        controls.addView(zoomBar);
        controls.addView(text("Position — drag the image in the preview", 13, Color.WHITE));
        addSpace(controls, 4);
        controls.addView(button("ROTATE / IMAGE CORRECTION", () -> {
            rotation = (rotation + 90) % 360; applyTransform();
        }));
        addSpace(controls, 6);
        Switch loopSwitch = new Switch(this); loopSwitch.setText("Loop playback");
        loopSwitch.setTextColor(Color.WHITE); loopSwitch.setChecked(true);
        loopSwitch.setOnCheckedChangeListener((v, checked) -> {
            loop = checked; if (player != null) player.setRepeatMode(loop ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
        });
        controls.addView(loopSwitch);
        Switch correctionSwitch = new Switch(this); correctionSwitch.setText("Image correction");
        correctionSwitch.setTextColor(Color.WHITE); correctionSwitch.setChecked(true);
        correctionSwitch.setOnCheckedChangeListener((v, checked) -> { correction = checked; applyTransform(); });
        controls.addView(correctionSwitch);
        Switch floatingSwitch = new Switch(this); floatingSwitch.setText("Floating mini preview");
        floatingSwitch.setTextColor(Color.WHITE);
        floatingSwitch.setOnCheckedChangeListener((v, checked) -> setFloating(checked));
        controls.addView(floatingSwitch);
        page.addView(controls);
        addSpace(page, 16);
        TextView note = text("This app previews selected media inside the app. It does not replace the Android system camera.", 11, 0xFFB9C9F4);
        page.addView(note);
        setContentView(scroll);

        previewFrame.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { downX=e.getX()-dx; downY=e.getY()-dy; return true; }
            if (e.getAction() == MotionEvent.ACTION_MOVE) { dx=e.getX()-downX; dy=e.getY()-downY; applyTransform(); return true; }
            return true;
        });
        modes.setOnCheckedChangeListener((g, id) -> {
            boolean stream = id == streamMode.getId();
            rtmpInput.setVisibility(stream ? View.VISIBLE : View.GONE);
        });
        rtmpInput.setVisibility(View.GONE);
    }

    private void pickMedia() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(i, 41);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 41 && result == RESULT_OK && data != null && data.getData() != null) {
            selectedUri = data.getData();
            getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String type = getContentResolver().getType(selectedUri);
            isVideo = type != null && type.startsWith("video/");
            fileLabel.setText(selectedUri.getLastPathSegment());
            stopPlayer();
            ((View)previewFrame.getTag()).setVisibility(View.GONE);
            if (isVideo) {
                imageView.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                player = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(player);
                player.setMediaItem(MediaItem.fromUri(selectedUri));
                player.setRepeatMode(loop ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
                player.prepare(); player.play();
            } else {
                playerView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageURI(selectedUri);
                zoom=1f; dx=0; dy=0; rotation=0; applyTransform();
            }
        }
    }

    private void startRtmp() {
        String url = rtmpInput.getText().toString().trim();
        if (!url.startsWith("rtmp://") && !url.startsWith("rtmps://")) {
            Toast.makeText(this, "Enter a valid RTMP stream URL", Toast.LENGTH_SHORT).show(); return;
        }
        try {
            ((View)previewFrame.getTag()).setVisibility(View.GONE);
            imageView.setVisibility(View.GONE); playerView.setVisibility(View.VISIBLE);
            stopPlayer();
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            MediaItem item = MediaItem.fromUri(url);
            ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(new RtmpDataSource.Factory()).createMediaSource(item);
            player.setMediaSource(source);
            player.setRepeatMode(loop ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
            player.prepare(); player.play();
            fileLabel.setText("RTMP stream preview");
        } catch (Exception ex) {
            Toast.makeText(this, "Could not start stream: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void applyTransform() {
        if (imageView == null) return;
        imageView.setPivotX(imageView.getWidth()/2f);
        imageView.setPivotY(imageView.getHeight()/2f);
        imageView.setScaleX(zoom); imageView.setScaleY(zoom);
        imageView.setTranslationX(dx); imageView.setTranslationY(dy);
        imageView.setRotation(correction ? rotation : 0);
    }

    private void setFloating(boolean enabled) {
        floating = enabled;
        LinearLayout.LayoutParams lp;
        if (enabled) {
            lp = new LinearLayout.LayoutParams(160, 120);
            lp.gravity = Gravity.END;
            previewFrame.setLayoutParams(lp);
            previewFrame.setElevation(12f);
            Toast.makeText(this, "Compact preview enabled inside the app", Toast.LENGTH_SHORT).show();
        } else {
            lp = new LinearLayout.LayoutParams(-1, 230);
            previewFrame.setLayoutParams(lp);
            previewFrame.setElevation(0f);
        }
    }

    private void restorePreview() {
        stopPlayer();
        selectedUri = null; isVideo = false; zoom=1f; dx=0; dy=0; rotation=0;
        imageView.setImageDrawable(null); imageView.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);
        ((View)previewFrame.getTag()).setVisibility(View.VISIBLE);
        fileLabel.setText("No media selected");
    }

    private void stopPlayer() {
        if (player != null) { player.release(); player = null; }
        if (playerView != null) playerView.setPlayer(null);
    }

    @Override protected void onStop() { super.onStop(); if (player != null) player.pause(); }
    @Override protected void onDestroy() { stopPlayer(); super.onDestroy(); }
}
