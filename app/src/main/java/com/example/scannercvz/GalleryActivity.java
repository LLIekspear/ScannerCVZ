package com.example.scannercvz;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.jtransforms.fft.DoubleFFT_2D;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
    TextView textView;
    int[] indexX=new int[256];
    int[] indexY=new int[256];

    //метод, вызываемый при создании данного фрагмента приложения. Назначает интерфейс, действия при нажатии на определенные кнопки и т.п.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        getSupportActionBar().hide();
        chooseView=findViewById(R.id.choosenScan);
        cropButton=findViewById(R.id.selectOther);
        preprocessButton=findViewById(R.id.preprocess);
        textView=findViewById(R.id.infoView2);
        cropButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1001);
                //TODO убрать коммент на продакшене
                //flag=2;
                //CropImage.activity(lastImgUri)
                //        .setCropShape(CropImageView.CropShape.RECTANGLE)
                //        .start(GalleryActivity.this);
            }
        });

        preprocessButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO сделать препроцессинг изображений: rgb->gray >0.15 => ЦВЗ имеется? Вышло повторить ЦВЗ, но не вышло его извлечь!
                Mat tmp=new Mat();
                Utils.bitmapToMat(imageMain, tmp);

                //int key=513232484;
                int[] info=getSettings();
                int key=info[0];
                int vectorLength=info[1];
                int u1=4;
                int v1=5;
                int u2=5;
                int v2=4;
                int N=8;

                //detection(imageMain);
                //
                Imgproc.resize(tmp, tmp, new Size(512, 512));

                Bitmap tmpImage1=Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(tmp, tmpImage1);
                File tempFile4 = new File(getApplicationContext().getExternalCacheDir(), "tmpImage.png");
                try (FileOutputStream out = new FileOutputStream(tempFile4)) {
                    tmpImage1.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //
                Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_BGR2GRAY);
                Mat padded = new Mat();                     //expand input image to optimal size
                int m = Core.getOptimalDFTSize( tmp.rows() );
                int n = Core.getOptimalDFTSize( tmp.cols() ); // on the border add zero values
                Core.copyMakeBorder(tmp, padded, 0, m-tmp.rows(), 0, n-tmp.cols(), Core.BORDER_CONSTANT, Scalar.all(0));
                List<Mat> planes = new ArrayList<Mat>();
                padded.convertTo(padded, CvType.CV_32F);
                planes.add(padded);
                planes.add(Mat.zeros(padded.size(), CvType.CV_32F));
                Mat complexI = new Mat();
                Core.merge(planes, complexI);         // Add to the expanded another plane with zeros
                Core.dft(complexI, complexI, Core.DFT_REAL_OUTPUT);         // this way the result may fit in the source matrix
                Mat dftOrig=complexI.clone();
                // compute the magnitude and switch to logarithmic scale
                // => log(1 + sqrt(Re(DFT(I))^2 + Im(DFT(I))^2))
                Core.split(complexI, planes);                               // planes.get(0) = Re(DFT(I)
                // planes.get(1) = Im(DFT(I))
                Core.magnitude(planes.get(0), planes.get(1), planes.get(0));// planes.get(0) = magnitude
                Mat magI = planes.get(0);
                Mat matOfOnes = Mat.ones(magI.size(), magI.type());
                Core.add(matOfOnes, magI, magI);         // switch to logarithmic scale
                Core.log(magI, magI);
                // crop the spectrum, if it has an odd number of rows or columns
                magI = magI.submat(new Rect(0, 0, magI.cols() & -2, magI.rows() & -2));
                // rearrange the quadrants of Fourier image  so that the origin is at the image center
                int cx = (magI.cols()/2);
                int cy = (magI.rows()/2);
                Mat q0 = new Mat(magI, new Rect(0, 0, cx, cy));   // Top-Left - Create a ROI per quadrant
                Mat q1 = new Mat(magI, new Rect(cx, 0, cx, cy));  // Top-Right
                Mat q2 = new Mat(magI, new Rect(0, cy, cx, cy));  // Bottom-Left
                Mat q3 = new Mat(magI, new Rect(cx, cy, cx, cy)); // Bottom-Right
                Mat tmp1 = new Mat();               // swap quadrants (Top-Left with Bottom-Right)
                q0.copyTo(tmp1);
                q3.copyTo(q0);
                tmp1.copyTo(q3);
                q1.copyTo(tmp1);                    // swap quadrant (Top-Right with Bottom-Left)
                q2.copyTo(q1);
                tmp1.copyTo(q2);
                magI.convertTo(magI, CvType.CV_8UC1);
                Core.normalize(magI, magI, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1); // Transform the matrix with float values
                // into a viewable image form (float between
                // values 0 and 255).

                LinearLayout layout=findViewById(R.id.bitmapsLayout);
                Bitmap image1 = Bitmap.createBitmap(magI.cols(), magI.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(magI, image1);
                ImageView imageView=new ImageView(GalleryActivity.this);
                layout.addView(imageView);
                imageView.setImageBitmap(image1);


                File tempFile2 = new File(getApplicationContext().getExternalCacheDir(), "tempImage_spectrum.png");
                try (FileOutputStream out = new FileOutputStream(tempFile2)) {
                    image1.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                //List<Integer> temp=randomVector(57846874321257L, 50);
                List<Integer> temp=Arrays.asList(1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1);
                //List<Integer> temp=Arrays.asList(1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0);
                //double[] genVectorDouble={1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1};
                double[] genVectorDouble={1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0};
                String line="";
                //for(int elem : temp) {
                //    line+=Integer.toString(elem)+" ";
                //}
                textView.setText(line);


                ///
                double maxVal=Core.minMaxLoc(magI).maxVal;
                // Найти центр изображения
                int centerX = (int)(/*tmp*/magI.size().width/2)+1;//321;//(magI.width()/2)+1;//(magI.cols() / 2)+1;
                int centerY = (int)(/*tmp*/magI.size().height/2)+1;//258;//(magI.height()/2)+1;//(magI.rows() / 2)+1;
                // Определить радиус окружности
                int radius1=(int)Math.min(tmp.size().height, tmp.size().width)/4;
                // Создать пустой вектор
                Mat mask=Mat.zeros(magI.size(), CvType.CV_8UC1);
                Imgproc.circle(mask, new Point(centerX, centerY), radius1, new Scalar(255), 1/*-1* - это будет полный круг*/);
                Mat magCircle=new Mat();
                magI.copyTo(magCircle, mask);

                Bitmap tmpBtmp=Bitmap.createBitmap(magI.width(), magI.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(magCircle, tmpBtmp);
                ImageView tmpView=new ImageView(GalleryActivity.this);
                layout.addView(tmpView);
                tmpView.setImageBitmap(tmpBtmp);

                File tempFile1 = new File(getApplicationContext().getExternalCacheDir(), "tempImage_original.png");
                try (FileOutputStream out = new FileOutputStream(tempFile1)) {
                    tmpBtmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Mat magCircleCopy=Mat.zeros(magI.size(), CvType.CV_8UC1);

                //magCircleCopy=makeWatermark(magCircleCopy.size(), radius, 57846874321257L, 50);

                List<Integer> vector = new ArrayList<Integer>();
                double[] testVector=new double[256];
                List<Integer> vector1=new ArrayList<Integer>();
               //TODO старая рабочая тема
                Mat forTest=magI.clone();

                for(int i=0; i</*360*/vectorLength; ++i) {
                    int x=centerX+(int)(radius1*Math.cos(i*2*Math.PI/vectorLength));
                    int y=centerY+(int)(radius1*Math.sin(i*2*Math.PI/vectorLength));
                    vector.add((int)(/*magCircle*/magI.get(y, x)[0])); //y,x - old, magCircle
                    testVector[i]=magI.get(y, x)[0];
                    forTest.put(y,x,0); //ПОДКРАСКА ДЛЯ ПОНИМАНИЯ
                    //vector1.add((int)(magCircleCopy.get(y, x)[0]));
                    //magCircleCopy.put((int)y, (int)x, 255);
                }

                Bitmap tmpBtmp3=Bitmap.createBitmap(magI.width(), magI.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(forTest, tmpBtmp3);
                File tempFile3 = new File(getApplicationContext().getExternalCacheDir(), "tempImage_coloredSpectrum.png");
                try (FileOutputStream out = new FileOutputStream(tempFile3)) {
                    tmpBtmp3.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                vector1=extractVector(magCircle/*magI*/, radius1, vectorLength/*360*/, new Point(centerX-2, centerY)); //TODO выходит что-то +- дельное, для кота граница оказалась >0.3
                magCircleCopy=makeWatermark(magCircleCopy.size(), radius1, 57846874321257L, vectorLength);
                Utils.matToBitmap(magCircleCopy, tmpBtmp);

                File tempFile = new File(getApplicationContext().getExternalCacheDir(), "tempImage_copy.png");
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    tmpBtmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //double step=0.01;
                //for(double theta=0; theta<=2*Math.PI; theta+=step) {
                //    double x=centerX+radius*Math.cos(theta);
                //    double y=centerY+radius*Math.sin(theta);
                //    vector.add((int)(magCircle.get((int)y, (int)x)[0]));
                //}

                // Пройти по всем точкам в пределах круга
                //for (int i = 0; i < 360; ++i) {
                //    double angle=i*Math.PI/180;
                //    int x=(int)Math.round(centerX+radius*Math.cos(angle)); //radius->vectorLength
                //    int y=(int)Math.round(centerY+radius*Math.sin(angle));
                //    vector.add((double)(magCircle.get(y, x)[0]/maxVal));
                //}

                line="";
                //TODO TEST
                //double testPixel=magI.get(406, 321)[0];//incorrect
                //double testPixel1= magI.get(321, 406)[0];
                double[] testPixels=new double[16];
                int[] locs=new int[2];
                int[] center={centerY, centerX};
                //Log.d("TEST_VALUE", "(25,17): "+Double.toString(complexI.get(25, 17)[0]));
                for(int i=0; i<16; ++i) {
                    locs=watermarkValue(center, i, 16, radius1);
                    testPixels[i]=magI.get(locs[0], locs[1])[0];
                }
                //line+="\n"+Double.toString(testPixel)+" "+Double.toString(testPixel1)+"\n";
                double max111=testPixels[0];
                double min111=testPixels[0];
                for(double elem : testPixels) {
                    if(elem>max111)
                        max111=elem;
                    else if(elem<min111)
                        min111=elem;
                }
                for(int i=0; i<256; ++i) {
                    //testPixels[i]=(testPixels[i]-min111)/(max111-min111);
                }

                //TODO для тестов значений пикселей, потом вернемся
                //for(int i=0; i<16; ++i) {
                //    line += Double.toString(testPixels[i]) + " ";
                //}
                //line+="\n";
                //

                //int[][] indices=new int[vectorLength][vectorLength];
                //int[] center={321, 258};
                //for(int t=0; t<vectorLength; ++t) {
                //    indices[t]=watermarkValue(center, t, vectorLength, radius);
                //}

                //
                double max11=vector.get(0);
                double min11=vector.get(0);
                for(int elem : vector) {
                    if(elem>max11)
                        max11=elem;
                    else if(elem<min11)
                        min11=elem;
                }
                for(int i=0; i<vector.size(); ++i) {
                    double val=((double)(vector.get(i))-min11)/(max11-min11);
                    vector.set(i, (int)Math.round(val));
                }

                //textView.append("\n");
                //for(int i=0; i<vector.size(); ++i) {
                //    textView.append(Double.toString(vector.get(i))+" ");
                //}
//                textView.append(Double.toString(correlation(temp, vector)));
//                textView.append("\n"+Double.toString(pearsonCorCoeff(genVectorDouble, testPixels))+"\n");
                //textView.append(Double.toString(testPixel1)+" "+Double.toString(testPixel));
                //textView.append("\n"+Double.toString(pearsonCorCoeff(genVectorDouble, testVector))+"\n");
                //textView.append("\n");
//                textView.append(Float.toString(correlationCoefficient(temp, vector, vector.size())));
                //textView.append("\n");
                //textView.append(Double.toString(correlationCoefficient(temp, vector1, vector1.size())));
                double max=vector1.get(0);
                double min=vector1.get(0);
                for(double elem : vector1) {
                    if(elem>max)
                        max=elem;
                    else if(elem<min)
                        min=elem;
                }
                for(int i=0; i<vector1.size(); ++i) {
                    vector1.set(i,(int)Math.round((vector1.get(i)-min)/(max-min)));
                }
                //List<Double> test=corrFromResource(temp, vector1, temp.size());
               // double test=corrFromResource(temp, vector1, temp.size());
                ////float test=correlationCoefficient(temp, vector1, vector1.size());
                ///line+="Norm. vector1: "+Float.toString(test)+" "+Double.toString(max)+" "+Double.toString(min)+" "+"\n";
                //line+=Integer.toString(centerX)+" "+Integer.toString(centerY);

                //line+="\n";
                //for(int elem : vector){//vector1) {
                    //line+=" "+Integer.toString(elem);
                //    line+=" "+Integer.toString(Math.round(elem));
                //}
                //textView.append(line);
                //textView.append(Double.toString(corrFromResource(temp, vector, temp.size())));

                try {
                    int dDCT=detectionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                    //extractionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                    if(dDCT==0) {
                        //ЦВЗ был обнаружен
                        Toast.makeText(GalleryActivity.this, "ЦВЗ был обнаружен и успешно извлечен в основном изображении!", Toast.LENGTH_SHORT).show();
                        extractionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                    } else if(dDCT==1) {
                        //ЦВЗ не обнаружен
                        Toast.makeText(GalleryActivity.this, "ЦВЗ не был обнаружен в основном изображении!", Toast.LENGTH_SHORT).show();
                    } else if(dDCT==2) {
                        //ошибка с размерами изображения или иная ошибка (не сохранилось временное изображение, ошибка в программе)
                        Toast.makeText(GalleryActivity.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                    }
                    for(ImageView elem : selectedImagesIds) {
                        tempFile4 = new File(getApplicationContext().getExternalCacheDir(), "tmpImage.png");
                        try (FileOutputStream out = new FileOutputStream(tempFile4)) {
                            BitmapDrawable bd=(BitmapDrawable) elem.getDrawable();
                            bd.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        dDCT=detectionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                        //dDCT=3;
                        if(dDCT==0) {
                            //ЦВЗ был обнаружен
                            Toast.makeText(GalleryActivity.this, "ЦВЗ был обнаружен и успешно извлечен!", Toast.LENGTH_SHORT).show();
                            extractionDCT(tempFile4.getPath(), key, vectorLength, u1, v1, u2, v2, N);
                        } else if(dDCT==1) {
                            //ЦВЗ не обнаружен
                            Toast.makeText(GalleryActivity.this, "ЦВЗ не был обнаружен!", Toast.LENGTH_SHORT).show();
                        } else if(dDCT==2) {
                            //ошибка с размерами изображения или иная ошибка (не сохранилось временное изображение, ошибка в программе)
                            Toast.makeText(GalleryActivity.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch(Exception error) {
                    Toast.makeText(GalleryActivity.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                }
                try {
                    double dDFT=detectionDFT(tempFile4.getPath(), key, vectorLength);
                    if(dDFT==0) {
                        //ЦВЗ был обнаружен
                        Toast.makeText(GalleryActivity.this, "ЦВЗ был обнаружен и успешно извлечен в основном изображении!", Toast.LENGTH_SHORT).show();
                        extractionDFT();
                    } else if(dDFT==1) {
                        //ЦВЗ не обнаружен
                        Toast.makeText(GalleryActivity.this, "ЦВЗ не был обнаружен в основном изображении!", Toast.LENGTH_SHORT).show();
                    } else if(dDFT==2) {
                        //ошибка с размерами изображения или иная ошибка (не сохранилось временное изображение, ошибка в программе)
                        Toast.makeText(GalleryActivity.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                    }
                    for(ImageView elem : selectedImagesIds) {
                        tempFile4 = new File(getApplicationContext().getExternalCacheDir(), "tmpImage.png");
                        try (FileOutputStream out = new FileOutputStream(tempFile4)) {
                            BitmapDrawable bd=(BitmapDrawable) elem.getDrawable();
                            bd.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        dDFT=detectionDFT(tempFile4.getPath(), key, vectorLength);
                        if(dDFT==0) {
                            //ЦВЗ был обнаружен
                            Toast.makeText(GalleryActivity.this, "ЦВЗ был обнаружен и успешно извлечен!", Toast.LENGTH_SHORT).show();
                            extractionDFT();
                        } else if(dDFT==1) {
                            //ЦВЗ не обнаружен
                            Toast.makeText(GalleryActivity.this, "ЦВЗ не был обнаружен!", Toast.LENGTH_SHORT).show();
                        } else if(dDFT==2) {
                            //ошибка с размерами изображения или иная ошибка (не сохранилось временное изображение, ошибка в программе)
                            Toast.makeText(GalleryActivity.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch(Exception error) {
                    error.printStackTrace();
                    Toast.makeText(GalleryActivity.this, "Возникла какая-то ошибка (проверьте права приложения или код программы)!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Intent choosePhotoIntent=new Intent(Intent.ACTION_PICK);
        //choosePhotoIntent.setType("image/*");
        //startActivityForResult(choosePhotoIntent, CHOOSE_PHOTO);
        startCropActivity();
    }

    public int[] generateRandomCVZ(int length, long key) {
        int[] res=new int[length];
        MersenneTwister mt=new MersenneTwister(key);
        Random random=new Random(key);
        UniformIntegerDistribution dis=new UniformIntegerDistribution(0, 1);
        String line="\n";
        for(int i=0; i<length; ++i) {
            res[i]=random.nextInt(2);//random.nextBoolean() ? 1 : 0;
            line+=Integer.toString(res[i])+" ";
        }
        textView.append(line);
        return res;
    }

    public double detectionDFT(String path, int key, int length) {
        //DFT Detection try
        Python py=Python.getInstance();
        PyObject pyobj=py.getModule("script");
        PyObject obj=pyobj.callAttr("dftDetect", path, key, length);
        double res=obj.toDouble();
        //textView.append("\n"+Double.toString(res)+"\n");
        if(res==0)
            textView.append("\nОбнаружен встроенный в изображение методом DFT ЦВЗ!");
        else
            textView.append("\nНе обнаружен встроенный в изображение методом DFT ЦВЗ!");
        return res;
    }

    public void extractionDFT() {
        //DFT Extraction try
    }

    public int detectionDCT(String path, int key, int length, int u1, int v1, int u2, int v2, int n) {
        Python py=Python.getInstance();
        PyObject pyobj=py.getModule("script");
        PyObject obj=pyobj.callAttr("dctDetect", path, key, length, u1, v1, u2, v2, n);
        int res=obj.toInt();
        //textView.append("\n"+Integer.toString(res)+"\n");
        if(res==0)
            textView.append("\nОбнаружен встроенный в изображение методом DCT ЦВЗ!");
        else
            textView.append("\nНе обнаружен встроенный в изображение методом DCT ЦВЗ!");
        return res;
    }
    public void extractionDCT(String path, int key, int length, int u1, int v1, int u2, int v2, int n) {
        Python py=Python.getInstance();
        PyObject pyobj=py.getModule("script");
        PyObject obj=pyobj.callAttr("dctExtract", path, key, length, u1, v1, u2, v2, n);
        textView.append("\n"+obj.toString()+"\n");
    }

    public List<Integer> extractVector(Mat image, int radius, int vectorLength, Point point) {
        List<Integer> vector=new ArrayList<Integer>();
        //int[] center={(int)(image.size().height/2)+1, (int)(image.size().width/2)+1};
        int[] center={(int)point.x, (int)point.y};
        for(int i=0; i</*360*/vectorLength; ++i) {
            int x=center[0]+(int)(radius*Math.cos(i*2*Math.PI/vectorLength));
            int y=center[1]+(int)(radius*Math.sin(i*2*Math.PI/vectorLength));
            vector.add((int)(image.get(y, x)[0])); //y,x
        }
        return vector;
    }
//TODO makeWatermark и watermarkValue работают штатно!!!
    public Mat makeWatermark(Size imageShape, int radius, long secretKey, int vectorLength) {
        Mat watermark=Mat.zeros(imageShape, 0);
        int[] center={(int)(imageShape.height/2)+1/*258*/, (int)(imageShape.width/2)+1/*321*/};
        int[] vector={1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1};
        int[][] indices=new int[vectorLength][vectorLength];
        for(int t=0; t<vectorLength; ++t) {
            indices[t]=watermarkValue(center, t, vectorLength, radius);
        }

        for(int i=0; i<vectorLength; ++i) {
            int[] location=indices[i];
            watermark.put(location[0], location[1], vector[i]*255);
            indexX[i]=location[0];
            indexY[i]=location[1];
        }
        return watermark;
    }
    public int[] watermarkValue(int[] center, int value, int vectorLength, int radius) {
        int [] indic={center[0]+(int)(radius*Math.cos(2*value*Math.PI/vectorLength)),center[1]+(int)(radius*Math.sin(2*value*Math.PI/vectorLength))};
        //return (int)(center[0]+radius*Math.cos(2*value*Math.PI/vectorLength))*(int)(center[1]+radius*Math.sin(2*value*Math.PI/vectorLength));
        return indic;
    }

    public double correlation(List<Integer> x, List<Integer> y) {
        int n=x.size();
        double meanX=meanInteger(x);
        double meanY=meanInteger(y);
        double stdX=deviationInteger(x, meanX);
        double stdY=deviationInteger(y, meanY);
        double sum=0;
        for(int i=0; i<n; ++i) {
            sum+=(x.get(i)-meanX)*(y.get(i)-meanY);
        }
        return sum/(n*stdX*stdY);
    }
    public double meanDouble(List<Double> values) {
        double sum=0;
        for(double value : values) {
            sum+=value;
        }
        return sum/values.size();
    }
    public double meanInteger(List<Integer> values) {
        double sum=0;
        for(double value : values) {
            sum+=value;
        }
        return sum/values.size();
    }

    public Bitmap applyFFT(Bitmap img) {
        int width=img.getWidth();
        int height=img.getHeight();

        double[][] pixels=new double[height][width];
        for(int i=0; i<height; ++i) {
            for(int j=0; j<width; ++j) {
                int color=img.getPixel(j, i);
                pixels[i][j]=(0.299*Color.red(color)+0.587*Color.green(color)+0.114*Color.blue(color))/255.0;
            }
        }
        DoubleFFT_2D fft=new DoubleFFT_2D(pixels.length, pixels[0].length);
        fft.realForwardFull(pixels);
        for(int i=0;i<pixels.length; ++i) {
            for(int j=0; j<pixels[i].length; ++j) {
                int x=(i+ pixels.length/2)%pixels.length;
                int y=(j+pixels[i].length/2)%pixels[i].length;
                double temp=pixels[i][j];
                pixels[i][j]=pixels[x][y];
                pixels[x][y]=temp;
            }
        }
        double val=pixels[321][406];
        Log.d("FFFTFFFTFFFT", "Value in pixel is "+ Double.toString(val));
        Bitmap res=Bitmap.createBitmap(pixels[0].length, pixels.length, Bitmap.Config.ARGB_8888);
        return res;
    }
    public double pearsonCorCoeff(double[] a, double[] b) {
        PearsonsCorrelation pc=new PearsonsCorrelation();
        double cor=pc.correlation(a, b);
        return cor;
    }

    public double deviationDouble(List<Double> values, double mean) {
        double sum=0;
        for(double value : values) {
            sum+=Math.pow(value-mean, 2);
        }
        double variance=sum/values.size();
        return Math.sqrt(variance);
    }
    public double deviationInteger(List<Integer> values, double mean) {
        double sum=0;
        for(double value : values) {
            sum+=Math.pow(value-mean, 2);
        }
        double variance=sum/values.size();
        return Math.sqrt(variance);
    }

    public float correlationCoefficient(List<Integer> X, List<Integer> Y, int n) {
        float sum_X = 0, sum_Y = 0, sum_XY = 0;
        float squareSum_X = 0, squareSum_Y = 0;
        for (int i = 0; i < n; i++) {
            // sum of elements of array X.
            sum_X = sum_X + X.get(i);
            // sum of elements of array Y.
            sum_Y = sum_Y + Y.get(i);
            // sum of X[i] * Y[i].
            sum_XY = sum_XY + X.get(i) * Y.get(i);
            // sum of square of array elements.
            squareSum_X = squareSum_X + X.get(i) * X.get(i);
            squareSum_Y = squareSum_Y + Y.get(i) * Y.get(i);
        }
        // use formula for calculating correlation
        // coefficient.
        float corr = (float)(n * sum_XY - sum_X * sum_Y)/ (float)(Math.sqrt((n * squareSum_X - sum_X * sum_X) * (n * squareSum_Y - sum_Y * sum_Y)));
        return corr;
    }

    public double corrFromResource(List<Integer> X, List<Integer> Y, int size) {
        List<Double> corrs=new ArrayList<Double>();
        double meanX=meanInteger(X);
        double meanY=meanInteger(Y);
        for(int j=0; j<size; ++j) {
            double sumUp=0;
            double sumDownX=0;
            double sumDownY=0;
            for(int i=0; i<size; ++i) {
                sumUp+=(X.get(i)-meanX)*(Y.get(i+j)-meanY);
            }
            for(int i=0; i<size; ++i) {
                sumDownX+=(X.get(i)-meanX)*(X.get(i)-meanX);
                sumDownY+=(Y.get(i+j)-meanY)*(Y.get(i+j)-meanY);
            }
            corrs.add(sumUp/Math.sqrt(sumDownX*sumDownY));
        }
        double max=corrs.get(0);
        for(int i=1; i<size; ++i) {
            if(corrs.get(i)>max)
                max=corrs.get(i);
        }
        return max;
    }
    public List<Integer> randomVector(long secretKey, int vectorLength) {
        List<Integer> res=new ArrayList<Integer>();
        Random rand = new Random(secretKey);
        //rand.setSeed(secretKey);
        for(int i=0; i<vectorLength; ++i) {
            res.add(rand.nextInt(2));
        }

        return res;
    }
    //метод, реагирующий на возвращаемый данные от других фрагментов приложения
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                        imageMain = bitmap;
                        ImageView imageView=new ImageView(this);
                        LinearLayout layout=findViewById(R.id.bitmapsLayout);
                        layout.addView(imageView);
                        imageView.setImageBitmap(imageMain);
                    } catch (Exception error) {
                        //
                    }
                }
            }
        }
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

    //метод для запуска окна обрезки изображения (вырезка определенной области)
    public void startCropActivity() {
        flag=1;
        CropImage.activity()
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .start(this);
        //was .setGuidelines(CropImageView.Guidelines.ON)
    }

    //метод для возврата в главное окно приложения
    public void returnFromGalleryClick() {
        finish();
    }

    //метод для устранения шумов и теней с отсканированного документа
    public void toGrayColor() {
        //BitmapDrawable bd=(BitmapDrawable) chooseView.getDrawable();
        //Bitmap bmp=bd.getBitmap();
        Bitmap bmp=imageMain;

        Mat imageMat=new Mat();
        Utils.bitmapToMat(bmp, imageMat);

        //Эта часть отвечает за осветление теневых областей и за возврат к цветной версии
        Mat mat=new Mat();
        Imgproc.cvtColor(imageMat, mat, Imgproc.COLOR_BGR2HSV);
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
        final Bitmap resBitmap=Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(res_norm, resBitmap);
        bmp=resBitmap;
        //избавляемся от шумов после выравнивания освещения
        Imgproc.GaussianBlur(res_norm, res_norm, new Size(3, 3), 0, 0);
        //
        Mat grayImage=new Mat();
        imageMat=res_norm.clone();
        Imgproc.cvtColor(imageMat, grayImage, Imgproc.COLOR_BGR2GRAY);
////        Imgproc.cvtColor(res_norm, grayImage, Imgproc.COLOR_BGR2GRAY);

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
            //
            Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_BGR2GRAY);
            //
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
////                Bitmap oldBitmap=resBitmap;
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
////            imageView.setImageBitmap(resBitmap);
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
}