package com.example.scannercvz;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

//TODO почитать у theartofdev android crop image про возможность CropShape, это не по углам выделение?
public class GalleryActivity extends AppCompatActivity {

    private final int CHOOSE_PHOTO=3;
    ImageView chooseView;
    Bitmap imageMain;
    Button cropButton;
    Button preprocessButton;
    List<ImageView> selectedImagesIds=new ArrayList<>();
    Uri lastImgUri;
    int flag=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        getSupportActionBar().hide();
        chooseView=findViewById(R.id.choosenScan);
        cropButton=findViewById(R.id.selectOther);
        preprocessButton=findViewById(R.id.preprocess);
        cropButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag=2;
                CropImage.activity(lastImgUri)
                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                        .start(GalleryActivity.this);
            }
        });

        preprocessButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO сделать препроцессинг изображений: rgb->gray
            }
        });

        //Intent choosePhotoIntent=new Intent(Intent.ACTION_PICK);
        //choosePhotoIntent.setType("image/*");
        //startActivityForResult(choosePhotoIntent, CHOOSE_PHOTO);
        startCropActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult imageRes=CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK&&flag==1) {
                try {
                    Uri imageUri = imageRes.getUri();
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    Bitmap image = BitmapFactory.decodeStream(imageStream);
                    //chooseView.setImageBitmap(image);
                    imageMain=image;
                    lastImgUri=imageUri;
                    toGrayColor();
                } catch(Exception error) {
                    error.printStackTrace();
                }
            } else if(resultCode==RESULT_OK&&flag==2) {
                try {
                    LinearLayout layout=findViewById(R.id.bitmapsLayout);
                    Uri imageUri = imageRes.getUri();
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    Bitmap image = BitmapFactory.decodeStream(imageStream);
                    ImageView imageView=new ImageView(this);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        Bitmap oldBitmap=image;
                        @Override
                        public void onClick(View v) {
                            imageView.setSelected(true);
                            //imageView.setImageResource(R.drawable.ic_launcher_foreground);
                            if(!selectedImagesIds.contains(imageView)) {
                                imageView.getDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
                                selectedImagesIds.add(imageView);
                            } else {
                                imageView.clearColorFilter();
                                imageView.setImageBitmap(oldBitmap);
                                selectedImagesIds.remove(imageView);
                            }
                            Log.i("IMGVIEWCLICK", "Selected!");
                        }
                    });
                    layout.addView(imageView);
                    imageView.setImageBitmap(image);
                } catch(Exception error) {
                    error.printStackTrace();
                }
            }
        }
        /*if(resultCode==RESULT_OK&&requestCode==CHOOSE_PHOTO) {
            try {
                Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap image= BitmapFactory.decodeStream(imageStream);
                chooseView.setImageBitmap(image);
            } catch(Exception error) {
                error.printStackTrace();
            }
        }*/
    }

    public void startCropActivity() {
        flag=1;
        CropImage.activity()
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .start(this);
        //was .setGuidelines(CropImageView.Guidelines.ON)
    }
    public void returnFromGalleryClick() {
        finish();
    }

    public void toGrayColor() {
        //BitmapDrawable bd=(BitmapDrawable) chooseView.getDrawable();
        //Bitmap bmp=bd.getBitmap();
        Bitmap bmp=imageMain;

        Mat imageMat=new Mat();
        Utils.bitmapToMat(bmp, imageMat);

        Mat grayImage=new Mat();
        Imgproc.cvtColor(imageMat, grayImage, Imgproc.COLOR_BGR2GRAY);

        Mat binaryImage=new Mat();
        Imgproc.adaptiveThreshold(grayImage, binaryImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10);

        List<MatOfPoint> contours=new ArrayList<>();
        Mat hierarchy=new Mat();
        Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfPoint> imageContours=new ArrayList<>();
        for(MatOfPoint contour : contours) {
            double area=Imgproc.contourArea(contour);
            if(area<2000) {
                continue;
            }
            Rect rect=Imgproc.boundingRect(contour);
            double aspectRatio=(double)rect.width/rect.height;
            if(aspectRatio<0.5||aspectRatio>2.0) {
                continue;
            }
            if(rect.x<0 || rect.y<0 || rect.x+rect.width>binaryImage.width() ||rect.y+rect.height>binaryImage.height()) {
                continue;
            }
            imageContours.add(contour);
        }

        List<Bitmap> imageAreas=new ArrayList<>();
        for(MatOfPoint contour : imageContours) {
            Rect rect=Imgproc.boundingRect(contour);
            Bitmap imageArea=Bitmap.createBitmap(rect.width, rect.height, Bitmap.Config.ARGB_8888);
            Mat tmp=new Mat();
            Utils.bitmapToMat(bmp, tmp);
            Utils.matToBitmap(new Mat(tmp, rect), imageArea);
            imageAreas.add(imageArea);
        }
        LinearLayout layout=findViewById(R.id.bitmapsLayout);
        for(Bitmap imageArea : imageAreas) {
            ImageView imageView=new ImageView(this);
            //imageView.setClickable(true);
            //imageView.setOnClickListener(this);
            //imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(80, 60));
            imageView.setOnClickListener(new View.OnClickListener() {
                Bitmap oldBitmap=imageArea;
                @Override
                public void onClick(View v) {
                    imageView.setSelected(true);
                    //imageView.setImageResource(R.drawable.ic_launcher_foreground);
                    if(!selectedImagesIds.contains(imageView)) {
                        imageView.getDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
                        selectedImagesIds.add(imageView);
                    } else {
                        imageView.clearColorFilter();
                        imageView.setImageBitmap(oldBitmap);
                        selectedImagesIds.remove(imageView);
                    }
                    Log.i("IMGVIEWCLICK", "Selected!");
                }
            });
            //imageView.setMaxHeight(20);
            //imageView.setMaxWidth(20);
            layout.addView(imageView);
            imageView.setImageBitmap(imageArea);
            //setContentView(imageView);
        }
        //chooseView.setImageBitmap(imageAreas.get(2));


        //green edges for all objects, works fine! with OpenCV
        /*Mat imageMat=new Mat();
        Utils.bitmapToMat(bmp, imageMat);
        Mat edgesMat=new Mat();
        Imgproc.Canny(imageMat, edgesMat, 100, 100);
        Mat colorEdges=new Mat();
        edgesMat.copyTo(colorEdges);
        Imgproc.cvtColor(colorEdges, colorEdges, COLOR_GRAY2BGRA);

        Scalar newColor = new Scalar(0,255,0);    //this will be green
        colorEdges.setTo(newColor, edgesMat);

        colorEdges.copyTo(imageMat, edgesMat);
        Utils.matToBitmap(imageMat, bmp);
        chooseView.setImageBitmap(bmp);*/

        //works fine without OpenCV to gray!
        /*ImageView imgView=findViewById(R.id.choosenScan);
        BitmapDrawable bd=(BitmapDrawable) imgView.getDrawable();
        Bitmap bmp=bd.getBitmap();
        Bitmap grayscale=Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas=new Canvas(grayscale);
        Paint paint=new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.RED);

        ColorMatrix colorMatrix=new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorFilter filter=new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(bmp, 0, 0, paint);
        imgView.setImageBitmap(grayscale);*/


        //ColorMatrix matrix=new ColorMatrix();
        //matrix.setSaturation(0);
        //Paint paint =new Paint();
        //paint.setColorFilter(new ColorMatrixColorFilter())
    }
}