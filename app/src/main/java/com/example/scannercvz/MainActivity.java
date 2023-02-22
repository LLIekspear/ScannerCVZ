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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.websitebeaver.documentscanner.DocumentScanner;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;

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

    //Mat test= new Mat();
    //Imgproc.find
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();
        croppedImageView=findViewById(R.id.cropped_image_view);
        versionInfo=findViewById(R.id.versionInfo);
        versionInfo.setText("Версия: "+BuildConfig.VERSION_NAME);

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Права на использование камеры уже имеются.", Toast.LENGTH_SHORT).show();
        } else {
            requestCameraPermission();
        }
        //documentScanner.startScan();
    }

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

    public void scanStart(View v) {
        Intent scannerActivity = new Intent(MainActivity.this, Scanner.class);
        //scannerActivity.putExtra("name", R.id.cropped_image_view);
        startActivity(scannerActivity);
    }

    public void openImage(View v) {
        Intent chooseActivity = new Intent(MainActivity.this, GalleryActivity.class);
        startActivity(chooseActivity);
    }

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

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}