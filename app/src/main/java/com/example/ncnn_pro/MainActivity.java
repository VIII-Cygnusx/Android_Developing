package com.example.ncnn_pro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.BitmapFactory;

import com.example.ncnn_pro.databinding.ActivityMainBinding;
import android.graphics.Bitmap;

import android.content.res.AssetManager;

import java.io.FileNotFoundException;


public class MainActivity extends AppCompatActivity {
    private static final int SELECT_IMAGE = 1;

    private TextView infoResult;
    private ImageView imageView;
    private Bitmap yourSelectedImage = null;

    private SqueezeNcnn squeezencnn = new SqueezeNcnn();
    // Used to load the 'ncnn_pro' library on application startup.


    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        infoResult = findViewById(R.id.infoResult);
        imageView = findViewById(R.id.imageView);



        //初始化检测
        boolean ret_init = squeezencnn.Init(getAssets());
        if (!ret_init)
        {
            infoResult.setText("ERROR");
        }

        //可以点击之后在相册选择图片
        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });


        //CPU 推理
        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                String result = squeezencnn.Detect(yourSelectedImage, false);

                if (result == null)
                {
                    infoResult.setText("detect failed");
                }
                else
                {
                    infoResult.setText(result);
                }
            }
        });


        //GPU 推理
        Button buttonDetectGPU = (Button) findViewById(R.id.buttonDetectGPU);
        buttonDetectGPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                String result = squeezencnn.Detect(yourSelectedImage, true);

                if (result == null)
                {
                    infoResult.setText("detect failed");
                }
                else
                {
                    infoResult.setText(result);
                }
            }
        });

    }


    //获得图片后预处理再显示
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            try
            {
                if (requestCode == SELECT_IMAGE) {
                    //缩放图片
                    Bitmap bitmap = decodeUri(selectedImage);
                    //使用ARGB_8888配置，这是一种常见的颜色格式，每个颜色通道（红、绿、蓝、透明度）都有8位深度
                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                    //rgba的缩放版本 resize to 227x227
                    yourSelectedImage = Bitmap.createScaledBitmap(rgba, 227, 227, false);
                    //由于已经创建了缩放后的Bitmap，并且不再需要原始的rgba，因此回收其内存以避免内存泄漏
                    rgba.recycle();

                    imageView.setImageBitmap(bitmap);
                }
            }
            catch (FileNotFoundException e)
            {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
    }

    //缩放图片
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException
    {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

}