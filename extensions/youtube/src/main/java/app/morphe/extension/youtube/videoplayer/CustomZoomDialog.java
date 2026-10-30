/*
 * Experimental custom zoom UI for Morphe. GPLv3-compatible contribution.
 */
package app.morphe.extension.youtube.videoplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.youtube.patches.FullscreenVideoScalePatch;
import app.morphe.extension.youtube.settings.Settings;

/** Fullscreen player zoom slider; a decimal field supports 1.122222x exactly
 * to the precision of a 32-bit floating-point setting. */
public final class CustomZoomDialog {
    private CustomZoomDialog() {}

    public static void show(Context context) {
        try {
            final float initial = Math.max(1f, Math.min(1.5f,
                    Settings.FULLSCREEN_PARTIAL_ZOOM_FACTOR.get()));
            final float density = context.getResources().getDisplayMetrics().density;
            final int padding = Math.round(20f * density);

            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(padding, padding, padding, padding);

            TextView hint = new TextView(context);
            hint.setText("Zoom multiplier (1.000000 - 1.500000). Type a precise value below.");
            content.addView(hint);

            SeekBar slider = new SeekBar(context);
            slider.setMax(5000); // 0.0001 increments from 1.0000x to 1.5000x
            slider.setProgress(Math.round((initial - 1f) * 10000f));
            LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            sliderParams.topMargin = Math.round(12f * density);
            content.addView(slider, sliderParams);

            EditText number = new EditText(context);
            number.setSingleLine(true);
            number.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            number.setImeOptions(EditorInfo.IME_ACTION_DONE);
            number.setSelectAllOnFocus(true);
            number.setText(String.format(Locale.US, "%.6f", initial));
            content.addView(number);

            final boolean[] updatingNumber = {false};
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    float factor = 1f + (progress / 10000f);
                    saveFactor(factor);
                    updatingNumber[0] = true;
                    number.setText(String.format(Locale.US, "%.6f", factor));
                    number.setSelection(number.length());
                    updatingNumber[0] = false;
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            number.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable text) {
                    if (updatingNumber[0]) return;
                    try {
                        float factor = Float.parseFloat(text.toString().trim());
                        if (!Float.isFinite(factor) || factor < 1f || factor > 1.5f) return;
                        saveFactor(factor);
                        // Programmatic slider change has fromUser=false, so the exact
                        // six-digit value in the edit field remains untouched.
                        slider.setProgress(Math.round((factor - 1f) * 10000f));
                    } catch (NumberFormatException ignored) {
                        // User is editing an incomplete number.
                    }
                }
            });

            new AlertDialog.Builder(context)
                    .setTitle("Custom video zoom")
                    .setView(content)
                    .setPositiveButton("Done", null)
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "CustomZoomDialog failure", ex);
        }
    }

    private static void saveFactor(float factor) {
        Settings.FULLSCREEN_PARTIAL_ZOOM_FACTOR.save(factor);
        FullscreenVideoScalePatch.applyScale();
    }
}
