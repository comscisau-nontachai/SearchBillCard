package dev.strubber.com.searchbillcard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.SparseArray
import android.view.SurfaceHolder
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
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
                                txt_result.append(qrcodes.valueAt(0).displayValue)
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
        var id = ""

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


            val query = """
                        select * from
                        (select distinct d1.no_billcard,d1.id,d1.part,d1.date,d1.qty,d1.remark,d1.Expr1,d1.sale_order,d2.partnumber,d2.de1,d2.de2,d2.de3,d2.de4
                        ,(DATEDIFF(day, d1.ts_create,GETDATE()))+1 as daywait,d1.ts_create
                        from ST_PRODUCTION..v_BillCardData as d1
                        LEFT JOIN ST_PRODUCTION..department_flow as d2 ON d2.id = d1.part
                        LEFT JOIN ST_PRODUCTION..production_process_master as d3 ON d3.no_billcard=d1.no_billcard
                        where d1.IsDelete='0' and d1.IsReceive='0' and d1.IsProd = '0' and d2.de1='1' and d3.no_billcard IS NULL
                        union
                        select distinct d1.no_billcard,d1.id,d1.part,d1.date,d1.qty,d1.remark,d1.Expr1,d1.sale_order,d2.partnumber,d2.de1,d2.de2,d2.de3,d2.de4
                        ,(DATEDIFF(day, d1.ts_create,GETDATE()))+1 as daywait,d1.ts_create
                        from ST_PRODUCTION..production_process_master as d1
                        LEFT JOIN ST_PRODUCTION..department_flow as d2 ON d2.id = d1.part
                        where d1.IsDelete='0' and d1.IsReceive='0' and d1.IsProd = '0' and d2.de1='1' ) as bill
                        where no_billcard LIKE '%$str%'
                        order by daywait desc
            """.trimIndent()

            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)



            while (rs.next()) {
                count++

                billcard_no = rs.getString("no_billcard")
                partnumber = rs.getString("Expr1")
                order_no = rs.getString("sale_order") ?: ""
                part_id = rs.getString("part")
                id = rs.getString("id")


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
                val intent = Intent(applicationContext, DetailActivity::class.java).apply {
                    putExtra("BILLCARD_NO", billcard_no)
                    putExtra("PARTNUMBER", partnumber)
                    putExtra("ORDER_NO", order_no)
                    putExtra("PART_ID", part_id)
                    putExtra("ID",id)
                }
                startActivity(intent)
                finish()
            }

        }
    }
}
