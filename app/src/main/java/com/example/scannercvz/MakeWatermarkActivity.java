package com.example.scannercvz;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.Objects;

public class MakeWatermarkActivity extends AppCompatActivity {

    private final int CHOOSE_PHOTO=1001;
    private final int MAKE_PHOTO=1002;
    private Button choosePhoto;
    private Button makePhoto;
    private TextView infoText;
    private int key=0;
    private int length=0;
    private int alg=0; //0 - DFT, 1 - DCT, 2 - LSB
    private Bitmap imageMain;

    public static final int STARTUP_DELAY=300;
    public static final int ANIM_ITEM_DURATION=1000;
    public static final int ITEM_DELAY=300;
    private boolean animationStarted=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        int[] info=new int[3];
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_watermark);

        choosePhoto=findViewById(R.id.makeWatermarkChoosePhotoButton);
        choosePhoto.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), CHOOSE_PHOTO);
            }
        });
        makePhoto=findViewById(R.id.makeWatermarkMakePhotoButton);
        makePhoto.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //intent.setType("image/*");
                //intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(Intent.createChooser(intent, "Make Picture"), MAKE_PHOTO);
                startActivityForResult(intent, MAKE_PHOTO);
            }
        });
        infoText=findViewById(R.id.makeWatermarkInfoText);

        info=getSettings();
        key=info[0];
        length=info[1];
        alg=info[2];


        /*Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), CHOOSE_PHOTO);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_PHOTO) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                        imageMain = bitmap;
                        Toast.makeText(MakeWatermarkActivity.this, "Успешно импортировано изображение!", Toast.LENGTH_SHORT).show();
                        setWatermark();
                    } catch (Exception error) {
                        error.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(MakeWatermarkActivity.this, "Не удалось импортировать изображение!", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode==MAKE_PHOTO) {
            if(resultCode==RESULT_OK) {
                if(data!=null) {
                    try {
                        Bundle extras = data.getExtras();
                        imageMain = (Bitmap) extras.get("data");
                        Toast.makeText(MakeWatermarkActivity.this, "Успешно сделан снимок!", Toast.LENGTH_SHORT).show();
                        setWatermark();
                    } catch(Exception error) {
                        //
                    }
                }
            } else {
                Toast.makeText(MakeWatermarkActivity.this, "Не удалось сделать снимок!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setWatermark() {
        Mat tmp=new Mat();
        Utils.bitmapToMat(imageMain, tmp);
        int u1=4;
        int v1=5;
        int u2=5;
        int v2=4;
        int N=8;
        int p=1;
        
        if(alg==0||alg==1)
            Imgproc.resize(tmp, tmp, new Size(512, 512));
        //else if(alg==2) {
            Bitmap tmpImage=Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp, tmpImage);
            File tempFile = new File(getApplicationContext().getExternalCacheDir(), "tmpImage.png");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                tmpImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Python py=Python.getInstance();
            PyObject pyobj=py.getModule("script");

            //Работает dft, работает lsb

            //String imageString=getStringImage(tmpImage);
            //PyObject obj = pyobj.callAttr("dftEncode", /*tempFile.getPath()*/imageString, 30000, key, length);//null;
            PyObject obj = null;//pyobj.callAttr("dftEncode", tempFile.getPath(), 30000, key, length);
            infoText.append("\n"+Integer.toString(alg));
            if(alg==0)
                obj=pyobj.callAttr("dftEncode", tempFile.getPath(), 30000, key, length);
                //Log.i("LOL", "temp");
            if(alg==1)
                obj=pyobj.callAttr("embedMessage", tempFile.getPath(), key, length, u1, v1, u2, v2, N, 1);
            else if(alg==2)
                obj=pyobj.callAttr("hide_data", tempFile.getPath(), key, length);
            double res=obj.toDouble();
            //String debug=obj.toString();
            //infoText.append("\n"+debug);
            //double res=-1;
            if(res==0) {
                infoText.append("\nНевидимый цифровой водяной знак успешно встроен в изображение методом преобразования Фурье!");
                saveToGallery(tempFile.getPath());
            } else if(res==1) {
                infoText.append("\nНевидимый цифровой водяной знак успешно встроен в изображение методом косинусного преобразования!");
                saveToGallery(tempFile.getPath());
            } else if(res==2) {
                infoText.append("\nНевидимый цифровой водяной знак успешно встроен в изображение методом LSB!");
                saveToGallery(tempFile.getPath());
            } else if(res== -1)
                infoText.append("\nНе удалось встроить невидимый цифровой водяной знак в изображение!");
        //}
    }

    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes=baos.toByteArray();
        String encodedImage=android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    public void saveToGallery(String path) {
        OutputStream fos;
        try {
            ContentResolver resolver=getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image"+".png");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            Uri imageUri= resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            Bitmap bmp= BitmapFactory.decodeFile(path);
            fos=resolver.openOutputStream(Objects.requireNonNull(imageUri));
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Objects.requireNonNull(fos);
        } catch(Exception error) {
            //
        }
    }
    public int[] getSettings() {
        File settingsFile = new File(getApplicationContext().getExternalCacheDir(), "settings.txt");
        int[] res=new int[3];
        try {
            if(!settingsFile.exists()) {
                String data = "0\n0\n0";
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(settingsFile));
                outputStreamWriter.write(data);
                outputStreamWriter.close();
            }
            BufferedReader br = new BufferedReader(new FileReader(settingsFile));
            for (int i = 0; i < 3; ++i) {
                res[i] = Integer.parseInt(br.readLine());
            }
            br.close();
        } catch (IOException e) {
            //
        }
        return res;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(!hasFocus||animationStarted) {
            return;
        }
        animate();
        super.onWindowFocusChanged(hasFocus);
    }
    public void animate() {
        ViewGroup container=(ViewGroup) findViewById(R.id.makeWatermarkMainLayout);

        for(int i=0; i<container.getChildCount(); ++i) {
            View v=container.getChildAt(i);
            ViewPropertyAnimatorCompat viewAnimator;
            if(!(v instanceof Button)) {
                viewAnimator= ViewCompat.animate(v)
                        .translationY(50).alpha(1)
                        .setStartDelay((ITEM_DELAY*i)+500)
                        .setDuration(1000);
            } else {
                viewAnimator=ViewCompat.animate(v)
                        .scaleY(1).scaleX(1)
                        .setStartDelay((ITEM_DELAY*i)+500)
                        .setDuration(500);
            }
            viewAnimator.setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    //метод для возврата в главное окно приложения
    public void returnFromGalleryClick() {
        finish();
    }
}