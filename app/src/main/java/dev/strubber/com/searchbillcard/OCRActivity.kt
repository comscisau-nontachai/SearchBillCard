package dev.strubber.com.searchbillcard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.widget.Toast
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.Text
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.activity_ocr.*

class OCRActivity : AppCompatActivity() {

    lateinit var  cameraSource: CameraSource
    lateinit var string: String
    val REQUEST_CAMERA_ID = 111
    lateinit var str_ocr :String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr)

        var textRecognizer = TextRecognizer.Builder(this).build()
        if(!textRecognizer.isOperational){
            Toast.makeText(this, "Detector not avaliable", Toast.LENGTH_SHORT).show();
        }else
        {
            cameraSource = CameraSource.Builder(this,textRecognizer).setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(20.0f)
                    .setAutoFocusEnabled(true)
                    .build()

            cameraPreview.holder.addCallback(object :SurfaceHolder.Callback{
                override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                    if (ContextCompat.checkSelfPermission(this@OCRActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@OCRActivity, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_ID)
                    } else {
                        cameraSource.start(cameraPreview.holder)
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder?) {
                    cameraSource.stop()
                }

                override fun surfaceCreated(holder: SurfaceHolder?) {
                }
            })
            textRecognizer.setProcessor(object :Detector.Processor<TextBlock>{
                override fun release() {

                }

                override fun receiveDetections(detections: Detector.Detections<TextBlock>?) {
                    val textItem = detections?.detectedItems
                    val builder = StringBuilder()
                    for(i in 0..Math.min(textItem!!.size().minus(1),5)){
                        builder.append(textItem.valueAt(i).value)
                        builder.append("\n")
                    }

                    txt_result.post{
                        txt_result.text = builder.toString()
                        str_ocr = builder.toString()
                    }

                }
            })
        }
        btn_search_by_ocr.setOnClickListener {
            Toast.makeText(this@OCRActivity, str_ocr, Toast.LENGTH_SHORT).show()
        }


    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_ID -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this@OCRActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@OCRActivity, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_ID)
                } else {
                    cameraSource.start(cameraPreview.holder)
                }
            }
        }

    }
}
