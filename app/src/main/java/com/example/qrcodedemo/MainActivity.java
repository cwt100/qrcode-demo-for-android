package com.example.qrcodedemo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_CAMERA_REQUEST_CODE = 1;
    private static final int REQUEST_CODE_IMAGE = 1; //跳轉至相簿的 request code.

    private TextView mTitleTextView;
    private TextView mMessageTextView;
    private Button mCameraButton;
    private Button mGalleryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
    }

    private void initUI() {
        mTitleTextView = (TextView) findViewById(R.id.textViewTitle);
        mMessageTextView = (TextView) findViewById(R.id.textViewMessage);
        mCameraButton = (Button) findViewById(R.id.buttonCamera);
        mGalleryButton = (Button) findViewById(R.id.buttonGallery);

        String applicationName = getApplicationName(this);
        mTitleTextView.setText(applicationName);
        mMessageTextView.setText("");
    }

    private static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public void onClickCamera(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST_CODE);
                return;
            }
        }

        // TODO: 2020/4/30 Open camer to scan QR Code.
        mMessageTextView.setText("");
        mCameraButton.setEnabled(true);
    }

    public void onClickGallery(View view) {

        mMessageTextView.setText("");
        Intent imagePickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(imagePickIntent, REQUEST_CODE_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: 2020/4/30 open camera to scan qr code.
                    mMessageTextView.setText("");
                    mCameraButton.setEnabled(true);
                }else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        //未允許，第二次詢問，需要dialog說明
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Camera permission");
                        builder.setMessage("Scan QR Code need to enable camera permission");
                        builder.setPositiveButton("Force setup", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST_CODE);
                            }
                        });
                        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mMessageTextView.setText("請至設定介面開啟相機權限");
                                mCameraButton.setEnabled(false);
                            }
                        });
                        builder.create();
                        builder.show();

                    }else {
                        //未允許，請至設定介面開啟相機權限
                        mMessageTextView.setText("請至設定介面開啟相機權限");
                        mCameraButton.setEnabled(false);
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_IMAGE:

                if (data == null) {
                    mMessageTextView.setText("Error: no picked image.");
                    return;
                }

                Uri uri = data.getData();
                Result result = parseQRCodeByUri(uri);
                if (result == null) {
                    // TODO: 2020/5/8 ERROR HANDLE: Decode QR Code error.
                    Log.d(TAG, "Decode QR Code error, result is null");
                    mMessageTextView.setText("Decode QR Code error, result is null");
                    return;
                }

                if (result.getText() == null) {
                    // TODO: 2020/5/8 ERROR HANDLE: Decode QR Code error.
                    Log.d(TAG, "Decode QR Code error, result getText is null");
                    mMessageTextView.setText("Decode QR Code error, result getText is null");
                    return;
                }

                Log.d(TAG, "Decode OK: " + result.getText().toString());
                mMessageTextView.setText("Decode result: \n" + result.getText().toString());
                break;
        }
    }

    private Result parseQRCodeByUri(Uri uri) {

        try {

            InputStream inputStream = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Log.d(TAG, "DEBUG: uri is not a bitmap, " + uri.toString());
                return null;
            }

            Hashtable<DecodeHintType, String> hints = new Hashtable<>();
            hints.put(DecodeHintType.CHARACTER_SET, "utf-8"); //設置二維碼內容的編碼
            Bitmap scanBitmap = Bitmap.createBitmap(bitmap);

            int px[] = new int[scanBitmap.getWidth() * scanBitmap.getHeight()];
            scanBitmap.getPixels(px, 0, scanBitmap.getWidth(), 0, 0, scanBitmap.getWidth(), scanBitmap.getHeight());
            RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap.getWidth(), scanBitmap.getHeight(), px);
            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));

            QRCodeReader reader = new QRCodeReader();
            try {
                return reader.decode(bBitmap, hints);
            } catch (NotFoundException e) {
                e.printStackTrace();
                Log.d(TAG, "DEBUG: reader NotFoundException");
                return null;
            } catch (ChecksumException e) {
                e.printStackTrace();
                Log.d(TAG, "DEBUG: reader ChecksumException");
                return null;
            } catch (FormatException e) {
                e.printStackTrace();
                Log.d(TAG, "DEBUG: FormatException");
                return null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG, "DEBUG: inputstream FileNotFoundException");
            return null;
        }
    }
}