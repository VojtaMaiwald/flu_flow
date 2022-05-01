package com.mai0042.flu_flow;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceEmotionsRecognition {

    private Interpreter interpreter;
    private int inputSize;
    private int height = 0;
    private int width = 0;

    private GpuDelegate gpuDelegate = null;
    private CascadeClassifier cascadeClassifier;

    FaceEmotionsRecognition(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException {
        this.inputSize = inputSize;
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModel(assetManager, modelPath), options);

        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt");
            FileOutputStream fileOutputStream = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int byteRead;

            while ((byteRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteRead);
            }

            inputStream.close();
            fileOutputStream.close();

            cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Mat recognizeImage(Mat image) {
        Core.flip(image.t(), image, -1);
        Mat imageGray = new Mat();
        Imgproc.cvtColor(image, imageGray, Imgproc.COLOR_RGBA2GRAY);

        height = imageGray.height();
        width = imageGray.width();

        int absoluteFaceSize = (int)(height * 0.1);
        MatOfRect faces = new MatOfRect();

        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(imageGray, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        Rect[] faceRectArray = faces.toArray();

        for (Rect rect : faceRectArray) {
            Imgproc.rectangle(image, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 2);
            Rect detection = new Rect((int)rect.tl().x, (int)rect.tl().y, (int)rect.br().x - (int)rect.tl().x, (int)rect.br().y - (int)rect.tl().y);
            Mat cropped = new Mat(image, detection);

            Bitmap bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, bitmap);

            Bitmap bitmap48x48 = Bitmap.createScaledBitmap(bitmap, 48, 48, false);

            ByteBuffer byteBuffer = convertBitmapToBuffer(bitmap48x48);

            float[][] emotionArray = new float[1][1];
            interpreter.run(byteBuffer, emotionArray);

            float emotion = (float)Array.get(Array.get(emotionArray, 0), 0);

            String emotionString = getEmotionDescription(emotion);

            Imgproc.putText(image, emotionString, new Point((int)rect.tl().x + 10, (int)rect.tl().y + 45), 1, 3.5f, new Scalar(0, 255, 0, 255), 2);
        }

        return image;
    }

    private String getEmotionDescription(float emotion) {
        String retVal = "";

        if (emotion >= 0.0f && emotion < 0.5f)
            retVal = "Surprise";
        else if (emotion >= 0.5f && emotion < 1.5f)
            retVal = "Fear";
        else if (emotion >= 1.5f && emotion < 2.5f)
            retVal = "Angry";
        else if (emotion >= 2.5f && emotion < 3.5f)
            retVal = "Neutral";
        else if (emotion >= 3.5f && emotion < 4.5f)
            retVal = "Sad";
        else if (emotion >= 4.5f && emotion < 5.5f)
            retVal = "Disgust";
        else
            retVal = "Happy";

        return  retVal;
    }

    private ByteBuffer convertBitmapToBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[inputSize * inputSize];

        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixelIndex = 0;
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                int pixel = pixels[pixelIndex++];

                byteBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((pixel & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    private MappedByteBuffer loadModel(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();

        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
