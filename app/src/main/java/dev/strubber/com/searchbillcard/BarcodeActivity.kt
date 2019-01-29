package dev.strubber.com.searchbillcard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import dev.strubber.com.searchbillcard.R.id.*
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_barcode.*

class BarcodeActivity : AppCompatActivity() {

    private val connectionDB = ConnectionDB()

    lateinit var barcodeDetector: BarcodeDetector
    lateinit var cameraSource: CameraSource
    val requestCameraPermissionID = 1010
    var count: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode)

        barcodeDetector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        cameraSource = CameraSource.Builder(this, barcodeDetector).setAutoFocusEnabled(true).setRequestedFps(20.0f).build()


        //add event
        cameraPreview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

                if (ContextCompat.checkSelfPermission(this@BarcodeActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@BarcodeActivity, arrayOf(Manifest.permission.CAMERA), requestCameraPermissionID)
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

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
            }

            override fun receiveDetections(p0: Detector.Detections<Barcode>?) {
                val qrcodes: SparseArray<Barcode> = p0!!.detectedItems

                if (qrcodes.size() != 0) {
                    txt_result.post {
                        if (qrcodes.valueAt(0) != null) {
                            count++
                            if (count == 1) {
                                //txt_result.append(qrcodes.valueAt(0).displayValue)
                                SearchFormScan(qrcodes.valueAt(0).displayValue).execute()
                                cameraSource.stop()
                            }
                        }
                    }
                }
            }
        })

        txt_back.setOnClickListener { finish() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            requestCameraPermissionID -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this@BarcodeActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@BarcodeActivity, arrayOf(Manifest.permission.CAMERA), requestCameraPermissionID)
                } else {
                    cameraSource.start(cameraPreview.holder)
                }
            }
        }

    }

    inner class SearchFormScan(val str: String?) : AsyncTask<Unit, Unit, String>() {

        var billcard_no = ""
        var partnumber = ""
        var order_no = ""
        var part_id = ""

        var count = 0

        lateinit var dialog: android.app.AlertDialog
        override fun onPreExecute() {
            super.onPreExecute()

            dialog = SpotsDialog.Builder()
                    .setContext(this@BarcodeActivity)
                    .setMessage("กำลังโหลดข้อมูล...")
                    .setCancelable(false)
                    .build()
                    .apply {
                        show()
                    }

        }

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select DISTINCT ppm.no_billcard,ppm.part,ppm.Expr1,\n" +
                    "CASE   \n" +
                    "      WHEN ppm.sale_order IS NULL THEN pp.sale_order   \n" +
                    "      WHEN ppm.sale_order IS NOT NULL THEN ppm.sale_order\n" +
                    "   END as sale_order1\n" +
                    "from ST_PRODUCTION..production_process_master ppm INNER JOIN ST_PRODUCTION..production_process pp ON ppm.no_billcard = pp.no_billcard\n" +
                    "where ppm.no_billcard = '$str' and ppm.IsDelete=0 and pp.IsDelete=0"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)



            while (rs.next()) {
                count++

                billcard_no = rs.getString("no_billcard")
                partnumber = rs.getString("Expr1")
                order_no = rs.getString("sale_order1")
                part_id = rs.getString("part")


            }
            conn.close()

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            dialog.dismiss()

            if (count < 1) {
                val pDialog = SweetAlertDialog(this@BarcodeActivity, SweetAlertDialog.WARNING_TYPE)
                pDialog.titleText = "ไม่พบหมายเลขบิลการ์ด..."
                pDialog.setCancelable(false)
                pDialog.confirmText = "ตกลง"
                pDialog.setConfirmClickListener {
                    it.dismissWithAnimation()
                    finish()
                }
                pDialog.show()

            } else if (count >= 1) {
                val intent = Intent(applicationContext, Detail2Activity::class.java)
                intent.putExtra("BILLCARD_NO", billcard_no)
                intent.putExtra("PARTNUMBER", partnumber)
                intent.putExtra("ORDER_NO", order_no)
                intent.putExtra("PART_ID", part_id)
                startActivity(intent)
                finish()
            }

        }
    }
}
