package com.example.scannercvz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.websitebeaver.documentscanner.DocumentScanner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Scanner extends AppCompatActivity {

    private static int TAKE_PICTURE_REQUEST = 1;
    Bitmap image;
    String uri;
    ImageView scan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner2);

        getSupportActionBar().hide();
        scan=findViewById(R.id.imageView);

        //все, что ниже, потом убрать, включая ветвление и первый вариант действия при true
       // boolean debug=true;
       // if(debug) {
        //  Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //    startActivityForResult(intent, TAKE_PICTURE_REQUEST);
        //} else {
            DocumentScanner documentScanner = new DocumentScanner(this, (croppedImageResults) -> {
                Bitmap image1 = BitmapFactory.decodeFile(croppedImageResults.get(0));

                scan.setImageBitmap(image1);

                File root = Environment.getExternalStorageDirectory();
                File cachePath = new File(root.getAbsolutePath() + "/DCIM/Camera/scan.png");
                try {
                    cachePath.delete();
                    cachePath.createNewFile();
                    FileOutputStream stream = new FileOutputStream(cachePath);
                    image1.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.close();
                    uri = cachePath.toString();
                    image = image1;

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }, (errorMessage) -> {
                Log.v("documentscannerlogs", errorMessage);
                return null;
            },
                    () -> {
                        Log.v("documentscannerlogs", "User canceled document scan");
                        return null;
                    },
                    null,
                    null,
                    null
            );

            documentScanner.startScan();
        //}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            // Проверяем, содержит ли результат маленькую картинку
            if (data != null) {
                if (data.hasExtra("data")) {
                    Bitmap thumbnailBitmap = data.getParcelableExtra("data");
                    // Какие-то действия с миниатюрой
                    scan.setImageBitmap(thumbnailBitmap);
                }
            }
        }
    }

    public void returnFromScanClick() {
        finish();
    }
}