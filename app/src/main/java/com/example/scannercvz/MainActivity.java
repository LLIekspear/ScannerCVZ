package com.example.scannercvz;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.websitebeaver.documentscanner.DocumentScanner;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.*;

import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGRA;

//TODO сделать документацию?
public class MainActivity extends AppCompatActivity {

    //<a href="https://www.flaticon.com/ru/free-icons/" title="владелец иконки">Владелец иконки от Freepik - Flaticon</a>
    //before in androidmanifest in app name was @string/app_name instead of Скане ЦВЗ
    //i used this library for scan with opencv - https://github.com/SDA-SE/document-scanner-android
    //and this library for crop the image (from gallery or from camera) - https://github.com/ArthurHub/Android-Image-Cropper/
    private ImageView croppedImageView;
    private final int CHOOSE_PHOTO=2;
    private final int CAMERA_PERMISSION_CODE=11;
    private final int WRITE_STORAGE_PERMISSION_CODE=12;
    private TextView versionInfo;
    private ImageButton imgButton;
    private Button makeWatermarkButton;

    public static final int STARTUP_DELAY=300;
    public static final int ANIM_ITEM_DURATION=1000;
    public static final int ITEM_DELAY=300;
    private boolean animationStarted=false;

    //Объект класса, который позволяет использовать уже готовый сканер документов
    DocumentScanner documentScanner=new DocumentScanner(this, (croppedImageResults) -> {
        Bitmap image=BitmapFactory.decodeFile(croppedImageResults.get(0));
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) croppedImageView.getLayoutParams();
        params.width=1000;//image.getWidth();
        params.height=1000;//image.getHeight();
        croppedImageView.setLayoutParams(params);
        croppedImageView.setImageBitmap(image);
    return null;
    }, (errorMessage) -> {
        Log.v("documentscannerlogs", errorMessage);
    return null;
    },
            () -> {
        Log.v("documentscannerlogs", "User canceled document scan.");
        return null;
            },
            null,
            null,
            null
    );

    //scanButton.setOnClickListener(new View.OnClickListener) {

    //}
    //Необходимое действие, чтобы убедиться в том, что OpenCV успешно был загружен, иначе он даже не будет работать
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    //imageMat=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    //Метод для выполнения действий при запуске приложения: установка действий для кнопок, назначение интерфейса и т.п.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!Python.isStarted())
            Python.start(new AndroidPlatform(this));
        Python py=Python.getInstance();
        PyObject pyobj=py.getModule("script");


        getSupportActionBar().hide();
        croppedImageView=findViewById(R.id.cropped_image_view);
        versionInfo=findViewById(R.id.versionInfo);
        versionInfo.setText("Версия: "+BuildConfig.VERSION_NAME);
        imgButton=findViewById(R.id.settingsButton);
        makeWatermarkButton=findViewById(R.id.makeWatermarkActivityButton);

        makeWatermarkButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent makeWatermarkActivity = new Intent(MainActivity.this, MakeWatermarkActivity.class);
                startActivity(makeWatermarkActivity);
            }
        });

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Права на использование камеры уже имеются.", Toast.LENGTH_SHORT).show();
        } else {
            requestCameraPermission();
        }
        //documentScanner.startScan();
        removeOldScans();
    }

    //Метод проверки и запроса прав на работу с камерой и внутренним хранилищем устройства, без которых приложение может работать некорректно
    private void requestCameraPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle("Запрос на получение прав")
                    .setMessage("Данные права нужны для корректной работы приложения.")
                    .setPositiveButton("Принять", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new  String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
                            ActivityCompat.requestPermissions(MainActivity.this, new  String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION_CODE);

                        }
                    })
                    .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new  String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
        }
    }

    //Ответ на случай, если права были получены, т.е. имеются, или если они не имеются, чтобы пользователь был об этом уведомлен
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==CAMERA_PERMISSION_CODE) {
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Права получены.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Права не были получены.", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode==WRITE_STORAGE_PERMISSION_CODE) {
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Права получены.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Права не были получены.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //метод для изменения настроек генерации ключа
    public void settingsSet(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Настройки ключа");
        alert.setMessage("Вы можете изменить ключ и длину генерируемого ЦВЗ");
        File settingsFile = new File(getApplicationContext().getExternalCacheDir(), "settings.txt");
        if(!settingsFile.exists()) {
            try {
                String data = "0\n0\n0";
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(settingsFile));
                outputStreamWriter.write(data);
                outputStreamWriter.close();
            } catch (IOException e) {
                //
            }
        }
        int key=0;
        int length=0;
        int alg=0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(settingsFile));
            int[] res=new int[3];
            for(int i=0; i<3; ++i) {
                res[i]=Integer.parseInt(br.readLine());
            }
            br.close();
            key=res[0];
            length=res[1];
            alg=res[2];
        }
        catch (IOException e) {
            //
        }
        final LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText inputKey = new EditText(MainActivity.this);
        inputKey.setText(Integer.toString(key));
        final EditText inputLength = new EditText(MainActivity.this);
        inputLength.setText(Integer.toString(length));
        final Spinner selectedAlg=new Spinner(MainActivity.this);

        String[] algs={"Фурье", "Косинусное", "LSB"};
        ArrayAdapter<String> adapter=new ArrayAdapter(MainActivity.this, android.R.layout.simple_spinner_item, algs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectedAlg.setAdapter(adapter);
        selectedAlg.setSelection(alg);
        AdapterView.OnItemSelectedListener itemSelectedListener=new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //String item=(String)adapterView.getItemAtPosition(i);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };

        layout.addView(inputKey);
        layout.addView(inputLength);
        layout.addView(selectedAlg);

        String selectedAlgString=(String)selectedAlg.getSelectedItem();

        alert.setView(layout);
        alert.setPositiveButton("Применить", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    int key=Integer.parseInt(String.valueOf(inputKey.getText()));
                    int length=Integer.parseInt(String.valueOf(inputLength.getText()));
                    int selectedAlgInteger=selectedAlg.getSelectedItemPosition();
                    String data = Integer.toString(key)+"\n"+Integer.toString(length)+"\n"+Integer.toString(selectedAlgInteger)+"\n";
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(settingsFile));
                    outputStreamWriter.write(data);
                    outputStreamWriter.close();
                } catch (IOException e) {
                    //
                }
                //String value = String.valueOf(getApplicationContext().getFilesDir().toString());
                //versionInfo.append("\n"+value);
            }
        });

        alert.setNegativeButton("Вернуться", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //метод начала сканирования, создает отдельное окно приложения
    public void scanStart(View v) {
        Intent scannerActivity = new Intent(MainActivity.this, Scanner.class);
        //scannerActivity.putExtra("name", R.id.cropped_image_view);
        startActivity(scannerActivity);
    }

    //метод открытия уже отсканированного документа или какого-то готового изображения, создает отдельное окно приложения
    public void openImage(View v) {
        Intent chooseActivity = new Intent(MainActivity.this, GalleryActivity.class);
        startActivity(chooseActivity);
    }

    //метод, который отвечает при возвращании каких-то данных после работы элементов приложения
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "Lol2", Toast.LENGTH_SHORT).show();
        if(resultCode==100) {
            String result=data.getStringExtra("SCAN");
            Uri uri=data.getData();
            if(result!=null) {
                uri = Uri.parse(result);
            }
            ImageView picView = (ImageView) findViewById(R.id.cropped_image_view);
            picView.setImageURI(uri);
//            byte[] byteArray=data.getByteArrayExtra("image");
//            croppedImageView.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
//           Bitmap byteArray=(Bitmap)data.getParcelableExtra("image");
//           croppedImageView.setImageBitmap(byteArray);
           Toast.makeText(this, "Lol", Toast.LENGTH_SHORT).show();
        }
        if(resultCode==RESULT_OK&&requestCode==CHOOSE_PHOTO) {
            try {
                Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap image=BitmapFactory.decodeStream(imageStream);

                //
                //InputStream is=getContentResolver().openInputStream(imageUri);
                Bitmap selectedBitmap=BitmapFactory.decodeStream(imageStream);
                Mat imageMat=new Mat();
                Utils.bitmapToMat(selectedBitmap, imageMat);
                Mat grayImage=new Mat();
                Imgproc.cvtColor(imageMat, grayImage, Imgproc.COLOR_BGR2GRAY);

                Mat edgesMat=new Mat();
                Imgproc.Canny(imageMat, edgesMat, 100, 100);
                Mat colorEdges=new Mat();
                edgesMat.copyTo(colorEdges);
                Imgproc.cvtColor(colorEdges, colorEdges, COLOR_GRAY2BGRA);
//step 2
                Scalar newColor = new Scalar(0,255,0);    //this will be green
                colorEdges.setTo(newColor, edgesMat);
//step 3
                colorEdges.copyTo(imageMat, edgesMat);
                Utils.matToBitmap(imageMat, selectedBitmap);
                croppedImageView.setImageBitmap(selectedBitmap);
                //Utils.matToBitmap(grayImage, selectedBitmap);
                //
//                Imgproc.GaussianBlur(imageMat, imageMat, new Size(3, 3), 0);
//                Imgproc.Canny(imageMat, imageMat, 100, 200);
//                Mat lines=new Mat();
//                Imgproc.HoughLinesP(imageMat, lines, 1, Math.PI/180, 50, 50, 10);
//                for(int i=0; i<lines.rows(); ++i) {
//                    double[] line=lines.get(i, 0);
//                    Imgproc.line(imageMat, new Point(line[0], line[1]), new Point(line[2], line[3]), new Scalar(255, 0, 0), 3);
//                }
//                Bitmap bmp=Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(imageMat, bmp);
//                croppedImageView.setImageBitmap(bmp);
                //Mat binary=new Mat(imageMat.rows(), imageMat.cols(), imageMat.type(), new Scalar(0));
                //Imgproc.threshold(grayImage, binary, 100,255, Imgproc.THRESH_BINARY_INV);
//
                //Mat edgesMat=new Mat();
                //Imgproc.Canny(grayImage, edgesMat, 50, 150);
                //List<MatOfPoint> contours=new ArrayList<>();
                //Mat hierarchy= new Mat();
                //Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                //List<Rect> rectangles=new ArrayList<>();
                //Scalar color=new Scalar(0, 0, 255);
                //for(int i=0; i<contours.size(); ++i) {
                    //Rect rect=Imgproc.boundingRect(contours.get(i));
                   // rectangles.add(rect);
                  //  Imgproc.drawContours(grayImage, contours, i, color, 2, Imgproc.LINE_8, hierarchy, 2, new Point());
//
                //}
                //Mat croppedMat=new Mat(grayImage, rectangles.get(1));
                //Utils.matToBitmap(grayImage, selectedBitmap);
               // croppedImageView.setImageBitmap(selectedBitmap);
                //
                //croppedImageView.setImageBitmap(image);
                Log.i("CROP", "Success!");
                Toast.makeText(this, "Lol1", Toast.LENGTH_SHORT).show();
            } catch(Exception error) {
                error.printStackTrace();
            }

        }
    }

    //Тоже необходимая часть для корректной работы OpenCV
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
        ViewGroup container=(ViewGroup) findViewById(R.id.linearLayout);

        ViewCompat.animate(imgButton)
                .translationY(-250)
                .setStartDelay(STARTUP_DELAY)
                .setDuration(ANIM_ITEM_DURATION).setInterpolator(
                        new DecelerateInterpolator(1.2f)).start();

        for(int i=0; i<container.getChildCount(); ++i) {
            View v=container.getChildAt(i);
            ViewPropertyAnimatorCompat viewAnimator=null;
            if(!(v instanceof Button)) {
                viewAnimator= ViewCompat.animate(v)
                        .translationY(50).alpha(1)
                        .setStartDelay((ITEM_DELAY*i)+500)
                        .setDuration(1000);
            } else if(!(v instanceof ImageButton)) {
                viewAnimator=ViewCompat.animate(v)
                        .scaleY(1).scaleX(1)
                        .setStartDelay((ITEM_DELAY*i)+500)
                        .setDuration(500);
            }
            viewAnimator.setInterpolator(new DecelerateInterpolator()).start();
        }
    }
    //метод для удаления старых сканов
    public void removeOldScans() {
        /*File dirScans=new File(getApplicationContext().getFilesDir().toString(), "Pictures");
        //versionInfo.append("\n"+dirScans.getPath());
        if (dirScans.isDirectory()) {
            String[] children = dirScans.list();
            for (int i = 0; i < children.length; ++i) {
                File file=new File(dirScans, children[i]);
                versionInfo.append("\n"+file.getPath());
                file.delete();
                if(file.exists()){
                    //file.getCanonicalFile().delete();
                    if(file.exists()){
                        getApplicationContext().deleteFile(file.getName());
                    }
                }
            }
        }*/
    }
}