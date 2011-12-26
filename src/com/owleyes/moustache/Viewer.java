package com.owleyes.moustache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout.LayoutParams;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectory;

public class Viewer extends Activity implements OnCheckedChangeListener {

    private static final String SEE_WARNING = "warning";

    private static final String CHECKED = "ignored_warning";

    private static final int SCALE = 0;

    private static final int ROTATE = 1;

    /** The list of drawable resource ids. */
    private static final int[] imageList = { R.drawable.one, R.drawable.two, R.drawable.three, R.drawable.four, R.drawable.five, R.drawable.six, R.drawable.seven, R.drawable.eight, R.drawable.nine,
            R.drawable.ten, R.drawable.eleven };

    /** The Linear Layout that contains all other elements. */
    private LinearLayout root_layout;

    /** ViewGroup to which we add moustaches. */
    private LinearLayout vg;

    /** The picture being viewed. */
    private ImageView iv;

    /** The Horizontal Scrollbar we use to display the images we can add. */
    private CustomHorizontalScrollView hsv;

    /** The RelativeLayout for the Scrollbar. */
    private CustomRelativeLayout rl;

    /** The remove button. */
    private Button _remove;

    /** The save button. */
    private Button _save;

    /** Shared Preferences for this app. */
    private SharedPreferences _preferences;

    /** An instance of SaveHelper I use to save images. */
    private SaveHelper _saver;

    private ToggleButton _mode;

    private Button _minus;

    private Button _plus;

    private int _state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _saver = new SaveHelper(this);
        _preferences = this.getSharedPreferences(Main.PREFS_FILE, 0);
        setContentView(R.layout.nothing);

        // Inflate all the views.
        init();

        Intent intent = getIntent();
        Uri imageURI = (Uri) intent.getParcelableExtra("image");

        addImage(imageURI);

        addDraggableImages();

    }

    /**
     * Populates the LienarLayout VG with the image resources from IMAGELIST.
     */
    private void addDraggableImages() {
        int counter = 0;
        for (int i : imageList) {
            CustomImageView temp = new CustomImageView(this);
            temp.setImageResource(i);
            temp.setId(counter);
            counter++;
            vg.addView(temp, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * Adds the image specified by the Uri IMAGEURI to the current view.
     * 
     */
    private void addImage(Uri imageURI) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(imageURI, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();

        iv.setImageBitmap(Viewer.setUpPicture(filePath));

        iv.invalidate();

    }

    /**
     * Called by the constructor to set up the views. Because we need to keep
     * track of a lot of the View elements, its easier for us to inflate each
     * View in the Class rather than in the XML. This is where that happens.
     */
    private void init() {
        root_layout = (LinearLayout) findViewById(R.id.root);

        rl = new CustomRelativeLayout(this);
        LayoutParams fill = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        LayoutParams wrap = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rl.setLayoutParams(fill);
        root_layout.addView(rl);

        iv = new ImageView(this);
        iv.setLayoutParams(wrap);

        rl.addView(iv);
        rl.setEditable(iv);

        _remove = new Button(this);
        LayoutParams removeLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        removeLP.addRule(RelativeLayout.ABOVE, 1);
        _remove.setLayoutParams(removeLP);
        _remove.setText("Remove");
        _remove.setId(2);
        _remove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rl.removeView(rl.getSelectedImage());
            }

        });
        rl.addView(_remove);

        _minus = new Button(this);
        LayoutParams minusLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        minusLP.addRule(RelativeLayout.ABOVE, 1);
        minusLP.addRule(RelativeLayout.LEFT_OF, 10);
        minusLP.bottomMargin = 6;
        minusLP.rightMargin = 10;
        _minus.setBackgroundResource(R.drawable.minus);
        _minus.setText("");
        _minus.setLayoutParams(minusLP);

        _minus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rl.handleMinusButton(_state);
            }
        });

        _plus = new Button(this);
        LayoutParams plusLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        plusLP.addRule(RelativeLayout.ABOVE, 1);
        plusLP.addRule(RelativeLayout.RIGHT_OF, 10);
        plusLP.bottomMargin = 6;
        plusLP.leftMargin = 10;
        _plus.setText("");
        _plus.setBackgroundResource(R.drawable.plus);
        _plus.setLayoutParams(plusLP);
        _plus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rl.handlePlusButton(_state);
            }
        });
        _mode = new ToggleButton(this);
        LayoutParams modeLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        modeLP.addRule(RelativeLayout.ABOVE, 1);
        modeLP.addRule(RelativeLayout.CENTER_HORIZONTAL);
        _mode.setLayoutParams(modeLP);
        _mode.setTextOn("Scale");
        _mode.setTextOff("Rotate");
        _mode.setChecked(true);
        _mode.setId(10);
        _mode.setOnCheckedChangeListener(this);

        _state = SCALE;
        rl.addView(_mode);

        rl.addView(_minus);
        rl.addView(_plus);

        _save = new Button(this);
        LayoutParams saveLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        saveLP.addRule(RelativeLayout.ABOVE, 1);
        saveLP.addRule(RelativeLayout.ALIGN_RIGHT, 1);
        _save.setLayoutParams(saveLP);
        _save.setText("Save");
        _save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rl.setDrawingCacheEnabled(true);
                final Bitmap b = rl.getDrawingCache();
                _saver.setBitmap(b);
                String warning = _preferences.getString(SEE_WARNING, "not_set");
                FileOutputStream fos = null;
                Log.e("WHAT!?", warning);
                if (!warning.equals(CHECKED)) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(Viewer.this);
                    LayoutInflater inflater = Viewer.this.getLayoutInflater();
                    View dialog = inflater.inflate(R.layout.save_location_warning, null);
                    alert.setView(dialog);
                    final CheckBox cb = (CheckBox) dialog.findViewById(R.id.ignore_warning);
                    alert.setNeutralButton("Okay", new Dialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                if (cb.isChecked()) {
                                    Log.e("WARNING", "Changin warning");
                                    Editor edit = Viewer.this._preferences.edit();
                                    edit.putString(SEE_WARNING, CHECKED);
                                    edit.commit();
                                }
                                _saver.setOutputStream(new FileOutputStream(Environment.getExternalStorageDirectory() + "/Pictures/image.png"));
                                _saver.setFilePath(Environment.getExternalStorageDirectory() + "/Pictures/image.jpg");
                                _saver.save();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                Toast.makeText(Viewer.this, "Error writing file, please try again", 1000).show();
                            }

                        }

                    });
                    alert.create().show();
                }
            }

        });
        rl.addView(_save);

        vg = new LinearLayout(this);

        hsv = new CustomHorizontalScrollView(this);
        hsv.setId(1);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        hsv.setBackgroundColor(Color.WHITE);
        hsv.setLayoutParams(lp);
        rl.addView(hsv);

        vg.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        hsv.addView(vg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        iv = null;
        vg = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * This function takes a filepath as a parameter and returns the
     * corresponding image, correctly scaled and rotated (if necessary)
     * 
     * @param filePath
     *            The filepath of the image
     * @return the scaled and rotated bitmap
     */
    private static Bitmap setUpPicture(String filePath) {

        int w = 512;
        int h = 384; // size that does not lead to OutOfMemoryException on Nexus
        // One
        Bitmap b = BitmapFactory.decodeFile(filePath);

        // Hack to determine whether the image is rotated
        boolean rotated = b.getWidth() > b.getHeight();

        Bitmap resultBmp = null;

        // If not rotated, just scale it
        int degree;
        if ((degree = degreeRotated(filePath)) == 0) {
            resultBmp = Bitmap.createScaledBitmap(b, w, h, true);
            b.recycle();
            b = null;
            // If rotated, scale it by switching width and height and then
            // rotated it
        } else {
            Bitmap scaledBmp = Bitmap.createScaledBitmap(b, w, h, true);
            b.recycle();
            b = null;
            Matrix mat = new Matrix();
            mat.postRotate(degree);
            resultBmp = Bitmap.createBitmap(scaledBmp, 0, 0, w, h, mat, true);

            // Release image resources
            scaledBmp.recycle();
            scaledBmp = null;
        }
        return resultBmp;
    }

    /**
     * RETURNS the number of degrees the image specified by FILEPATH is rotate
     * (i.e. what degree rotation the phone was at while taking the picture.
     * 
     */
    private static int degreeRotated(String filePath) {
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(new File(filePath));
            Directory exifDirectory = metadata.getDirectory(ExifDirectory.class);
            int orientation = exifDirectory.getInt(ExifDirectory.TAG_ORIENTATION);
            switch (orientation) {
                case 6:
                    return 90;
                case 8:
                    return 270;
                default:
                    return 0;

            }
        } catch (JpegProcessingException e1) {
            e1.printStackTrace();
        } catch (MetadataException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // This is the compound button
        if (isChecked) {
            /* Scale mode */
            _minus.setBackgroundResource(R.drawable.minus);
            _plus.setBackgroundResource(R.drawable.plus);
            _state = SCALE;
        } else {
            /* Rotate mode */
            _minus.setBackgroundResource(R.drawable.ctr_clk);
            _plus.setBackgroundResource(R.drawable.clk);
            _state = ROTATE;
        }

    }
}
