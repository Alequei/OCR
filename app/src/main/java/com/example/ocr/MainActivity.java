package com.example.ocr;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    SurfaceView mCameraView;
    TextView mTextView;
    CameraSource mCameraSource;
    private static final String TAG = "MainActivity";
    private static final int requestPermissionID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);

        startCameraSource();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == requestPermissionID) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finish();
                startActivity(new Intent(this, this.getClass()));
            }
        }
    }

    private void startCameraSource() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, requestPermissionID);
            return;
        }

        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Dependencias del detector aún no cargadas");
        } else {
            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();
            /**
             *
             * Agregue una llamada a SurfaceView y verifique si se otorga el permiso de la cámara.
             *              * Si se otorga el permiso, podemos iniciar nuestro CameraSource y pasarlo a surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });
            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }


                //Me da como resultado  6 digitos de  forma xyz-abc
                public String filtro(String placa) {
                    int pos = placa.indexOf("-");
                    String xyz, abc;
                    if (pos > 0) {
                        xyz = placa.substring(pos - 3, pos);
                        abc = placa.substring(pos + 1, pos + 3 + 1);
                        int primercaracter = xyz.charAt(0);
                        if (xyz.length() == 3 && abc.length() == 3) {
                            if ((primercaracter >= 65 && primercaracter <= 90) || (primercaracter >= 97 && primercaracter <= 122)) {
                                return xyz + "-" + abc;
                            }
                        }

                    }
                    return null;
                }

                /**
                 * Detecta todo el texto de la cámara usando TextBlock y los valores en un stringBuilder
                 * que luego se establecerá en el textView
                 * */
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = new Date();
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    final String fecha = dateFormat.format(date);
                    if (items.size() != 0) {
                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); i++) {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                String placabuscado = stringBuilder.toString() + "|";
                                mTextView.setText(filtro(guardar_linea_archivo(placabuscado+fecha)));

                            }
                        });
                    }
                }
            });
        }
    }

    public String guardar_linea_archivo(String s) {
        OutputStreamWriter escritor=null;

        try {
            escritor = new OutputStreamWriter(openFileOutput("placas.txt", Context.MODE_APPEND));
            escritor.write(s);
        } catch (Exception ex) {
            Log.e("ivan", "Error al escribir fichero a memoria interna");
        } finally {
            try {
                if (escritor != null)
                    escritor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return s;
    }


}