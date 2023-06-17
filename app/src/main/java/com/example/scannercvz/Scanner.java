package com.example.scannercvz;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.websitebeaver.documentscanner.DocumentScanner;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Scanner extends AppCompatActivity {

    private static int TAKE_PICTURE_REQUEST = 1;
    Bitmap image;
    String uri;
    ImageView scan;
    Button cropButton;
    Button proccessButton;
    TextView infoText;
    int flag=1;
    List<ImageView> selectedImagesIds=new ArrayList<>();

    public static final int STARTUP_DELAY=300;
    public static final int ANIM_ITEM_DURATION=1000;
    public static final int ITEM_DELAY=300;
    private boolean animationStarted=false;


    //метод, вызываемый при создании данного фрагмента приложения. Назначает интерфейс, действия при нажатии на определенные кнопки и т.п.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner2);

        getSupportActionBar().hide();
        infoText=findViewById(R.id.infoView);
        infoText.setText("");
        scan=findViewById(R.id.imageView);
        Button startProcessButton=findViewById(R.id.button);
        startProcessButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgExtract();
            }
        });

        cropButton=findViewById(R.id.areaSelection);
        cropButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag=2;
                // Создаем объект File для временного файла
                File tempFile = new File(getApplicationContext().getExternalCacheDir(), "tempImage.png");

                // Сохраняем Bitmap в файл
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    image.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Получаем URI из временного файла
                Uri uri = Uri.fromFile(tempFile);
                CropImage.activity(uri)
                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                        .start(Scanner.this);
            }
        });
        proccessButton=findViewById(R.id.button);
        startProcessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] res=getSettings();
                int key=res[0];
                int vectorLength=res[1];
                int u1=4;
                int v1=5;
                int u2=5;
                int v2=4;
                int N=8;
                try {
                    for(ImageView elem : selectedImagesIds) {
                        File tempFile4 = new File(getApplicationContext().getExternalCacheDir(), "tmpImage.png");
                        try (FileOutputStream out = new FileOutputStream(tempFile4)) {
                            BitmapDrawable bd=(BitmapDrawable) elem.getDrawable();
                            Mat tmpMat=new Mat();
                            Bitmap tmpBmp=bd.getBitmap();
                            Utils.bitmapToMat(tmpBmp, tmpMat);
                            Imgproc.resize(tmpMat, tmpMat, new Size(512, 512));
                            Utils.matToBitmap(tmpMat, tmpBmp);
                            //bd.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
                            tmpBmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        int dDCT=detectionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                        if(dDCT==0) {
                            //ЦВЗ был обнаружен
                            Toast.makeText(Scanner.this, "ЦВЗ был обнаружен и успешно извлечен!", Toast.LENGTH_SHORT).show();
                            extractionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                        } else if(dDCT==1) {
                            //ЦВЗ не обнаружен
                            Toast.makeText(Scanner.this, "ЦВЗ не был обнаружен!", Toast.LENGTH_SHORT).show();
                        } else if(dDCT==2) {
                            //ошибка с размерами изображения или иная ошибка (не сохранилось временное изображение, ошибка в программе)
                            Toast.makeText(Scanner.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                        }

                        double dDFT=detectionDFT(tempFile4.getPath(), key, vectorLength);
                        if(dDFT==0) {
                            //ЦВЗ был обнаружен
                            Toast.makeText(Scanner.this, "ЦВЗ был обнаружен и успешно извлечен!", Toast.LENGTH_SHORT).show();
                            //extractionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                        } else if(dDFT==1) {
                            //ЦВЗ не обнаружен
                            Toast.makeText(Scanner.this, "ЦВЗ не был обнаружен!", Toast.LENGTH_SHORT).show();
                        } else if(dDFT==2) {
                            //ошибка с размерами изображения или иная ошибка (не сохранилось временное изображение, ошибка в программе)
                            Toast.makeText(Scanner.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch(Exception error) {
                    Toast.makeText(Scanner.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //все, что ниже, потом убрать, включая ветвление и первый вариант действия при true
       // boolean debug=true;
       // if(debug) {
        //  Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //    startActivityForResult(intent, TAKE_PICTURE_REQUEST);
        //} else {
            DocumentScanner documentScanner = new DocumentScanner(this, (croppedImageResults) -> {
                Bitmap image1 = BitmapFactory.decodeFile(croppedImageResults.get(0));

                //устраняем тени
                //TODO оставить итоговое изображение в черно-белом формате или вырезать и осветлять отдельным методом нужную область на исходном изображении
                Mat mat=new Mat();
                Utils.bitmapToMat(image1, mat);

                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV);
                List<Mat> bgrPlanes=new ArrayList<>();
                List<Mat> list=new ArrayList<>();
                List<Mat> res=new ArrayList<>();

                Core.split(mat, bgrPlanes);
                list.add(bgrPlanes.get(2));
                res.add(0, bgrPlanes.get(0));
                res.add(1, bgrPlanes.get(1));
                for(Mat material : list) {
                    Mat dilatedImg=new Mat();
                    Mat kernel=Mat.ones(7, 7, CvType.CV_32F);
                    Imgproc.dilate(material, dilatedImg, kernel);
                    Imgproc.medianBlur(dilatedImg, dilatedImg, 21);
                    Mat diff=new Mat();
                    Core.absdiff(material, dilatedImg, diff);
                    Core.bitwise_not(diff, diff);
                    Mat norm=diff.clone();
                    Core.normalize(diff, norm, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
                    res.add(norm);
                }
                Mat res_norm=new Mat();
                Core.merge(res, res_norm);
                Imgproc.cvtColor(res_norm, res_norm, Imgproc.COLOR_HSV2BGR);
                final Bitmap resBitmap=Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                //избавляемся от шумов после выравнивания освещения
                Imgproc.GaussianBlur(res_norm, res_norm, new Size(3, 3), 0, 0);
                Utils.matToBitmap(res_norm, resBitmap);
                //scan.setImageBitmap(resBitmap);


                File root = Environment.getExternalStorageDirectory();
                File cachePath = new File(root.getAbsolutePath() + "/DCIM/Camera/scan.png");
                try {
                    cachePath.delete();
                    cachePath.createNewFile();
                    FileOutputStream stream = new FileOutputStream(cachePath);
                    image1.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.close();
                    uri = cachePath.toString();
                    //image = image1;
                    image=resBitmap;
                    imgExtract();
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



            //imgExtract();
        //}
    }

    //метод, реагирующий на возвращаемый данные от других фрагментов приложения
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK&&flag==1) {
            // Проверяем, содержит ли результат маленькую картинку
            if (data != null) {
                if (data.hasExtra("data")) {
                    Bitmap thumbnailBitmap = data.getParcelableExtra("data");

                    // Какие-то действия с миниатюрой
                    //scan.setImageBitmap(thumbnailBitmap); //оставляли для проверки полученного изображения
                    //imgExtract();
                }
            }
        } else if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult imageRes=CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK&&flag==2) {
                try {
                    LinearLayout layout = findViewById(R.id.bitmapsLayout);
                    Uri imageUri = imageRes.getUri();
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    Bitmap image = BitmapFactory.decodeStream(imageStream);
                    ImageView imageView = new ImageView(this);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        Bitmap oldBitmap = image;

                        @Override
                        public void onClick(View v) {
                            imageView.setSelected(true);
                            //imageView.setImageResource(R.drawable.ic_launcher_foreground);
                            if (!selectedImagesIds.contains(imageView)) {
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
                    Mat temp=new Mat();
                    Utils.bitmapToMat(image, temp);
                    Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);
                    Utils.matToBitmap(temp, image);
                    imageView.setImageBitmap(image);
                } catch (Exception error) {
                    error.printStackTrace();
                }
            }
        }
    }

    //метод возврата на главное окно приложения
    public void returnFromScanClick() {
        finish();
    }

    //метод извлечения всех прямоугольных областей из отсканированного изображения, который вероятней всего являются встроенными в документ изображениями
    public void imgExtract() {
        //Bitmap bmp=((BitmapDrawable)scan.getDrawable()).getBitmap();
        Bitmap bmp=image;

        Mat imageMat=new Mat();
        Utils.bitmapToMat(bmp, imageMat);

        Mat grayImage=new Mat();
        Imgproc.cvtColor(imageMat, grayImage, Imgproc.COLOR_BGR2GRAY);

        Mat binaryImage=new Mat();

        Imgproc.adaptiveThreshold(grayImage, binaryImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10);

        //
        /*Mat kernel=Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.morphologyEx(grayImage, grayImage, Imgproc.MORPH_CLOSE, kernel);*/

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
            /*TODO тут переводим в черно-белый формат, если же нужно извлечь фрагмент из исходного изображения,
               т.к. могут быть проблемы с дальнейшим распознаванием, теоретически, то просто сохраняем изначальный
               нетронутый вариант цветного изображения и извлекаем из него только нужные ПРЯМОУГОЛЬНИКИ (области) и
               уже потом переводим в черно-белый формат для распознавания ЦВЗ
             */
            //Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_BGR2GRAY);
            //
            Utils.matToBitmap(new Mat(tmp, rect), imageArea);
            imageAreas.add(imageArea);
        }
        LinearLayout layout=findViewById(R.id.bitmapsLayout);
        for(Bitmap imageArea : imageAreas) {
            ImageView imageView=new ImageView(this);
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
            layout.addView(imageView);
            imageView.setImageBitmap(imageArea);
        }
    }

    public int[] getSettings() {
        File settingsFile = new File(getApplicationContext().getExternalCacheDir(), "settings.txt");
        int[] res=new int[2];
        try {
            if(!settingsFile.exists()) {
                String data = "0\n0";
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(settingsFile));
                outputStreamWriter.write(data);
                outputStreamWriter.close();
            }
            BufferedReader br = new BufferedReader(new FileReader(settingsFile));
            for (int i = 0; i < 2; ++i) {
                res[i] = Integer.parseInt(br.readLine());
            }
            br.close();
        } catch (IOException e) {
            //
        }
        return res;
    }

    public int detectionDCT(String path, int key, int length, int u1, int v1, int u2, int v2, int n) {
        Python py=Python.getInstance();
        PyObject pyobj=py.getModule("script");
        PyObject obj=pyobj.callAttr("dctDetect", path, key, length, u1, v1, u2, v2, n);
        int res=obj.toInt();
        //textView.append("\n"+Integer.toString(res)+"\n");
        if(res==0)
            infoText.append("\nОбнаружен встроенный в изображение методом DСT ЦВЗ!");
        else
            infoText.append("\nНе обнаружен встроенный в изображение методом DСT ЦВЗ!");
        return res;
    }
    public void extractionDCT(String path, int key, int length, int u1, int v1, int u2, int v2, int n) {
        Python py=Python.getInstance();
        PyObject pyobj=py.getModule("script");
        PyObject obj=pyobj.callAttr("dctExtract", path, key, length, u1, v1, u2, v2, n);
        //textView.append("\n"+obj.toString()+"\n");
    }
    public double detectionDFT(String path, int key, int length) {
        //DFT Detection try
        Python py=Python.getInstance();
        PyObject pyobj=py.getModule("script");
        PyObject obj=pyobj.callAttr("dftDetect", path, key, length);
        double res=obj.toDouble();
        //textView.append("\n"+Double.toString(res)+"\n");
        if(res==0)
            infoText.append("\nОбнаружен встроенный в изображение методом DFT ЦВЗ!");
        else
            infoText.append("\nНе обнаружен встроенный в изображение методом DFT ЦВЗ!");
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
        ViewGroup container=(ViewGroup) findViewById(R.id.linearLayout3);

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
}