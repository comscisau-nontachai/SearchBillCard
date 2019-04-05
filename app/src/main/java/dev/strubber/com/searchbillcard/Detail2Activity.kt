package dev.strubber.com.searchbillcard

import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_detail2.*
import kotlinx.android.synthetic.main.item_process_product.view.*
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Detail2Activity : AppCompatActivity() {


    data class ProductProcessModel(var receive: String, var receive_note: String, var receive_date: String, var receive_name: String,
                                   var send: String, var send_note: String, var send_date: String, var send_name: String, var date_diff: String, var department: String)

    val listProductProcess: ArrayList<ProductProcessModel> = ArrayList()

    private val connectionDB = ConnectionDB()

    val loginID = MainActivity.loginId
    var isUserProcess1 = false
    var isUserProcess2 = false
    var isUserProcess3 = false
    var isUserProcess4 = false

    lateinit var dialog: android.app.AlertDialog

    var quantity = ""

    private var department = 0
    private var billCard = ""
    private var partNumber = ""
    private var orderNo = ""
    private var partId = ""
    private var id = ""

    val listNameProcess2: ArrayList<String> = ArrayList()
    val listNameProcess2ID: ArrayList<String> = ArrayList()

    //get part work flow
    private var dep1: String? = null
    private var dep2: String? = null
    private var dep3: String? = null
    private var dep4: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail2)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        billCard = intent.getStringExtra("BILLCARD_NO")
        partNumber = intent.getStringExtra("PARTNUMBER")
        orderNo = intent.getStringExtra("ORDER_NO")
        partId = intent.getStringExtra("PART_ID")
        id = intent.getStringExtra("ID")
        supportActionBar?.title = "หมายเลขบิลการ์ด $billCard"


        //Get Product Desc
        GetPartData(partId).execute()

        //Get Image product
        val http = "http://roomdatasoftware.strubberdata.com/pic_partnumber_new/get_product_image.php?part=$partNumber"
        loadImageProduct(http)

        //Get Department Desc
        GetDataDepartmentDesc(billCard, orderNo).execute()

        //get flow work
        GetFlowWork().execute()

        //Get Product Process 4 department
        GetProductProcess(billCard).execute()

        //Get Check Product Desc
        GetCheckProductDesc(billCard).execute()

        //check permission program
        CheckUserProcess(billCard).execute()

        //check ID can receive in process2
        CheckNameReceiveProcess2().execute()


    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    //rec-send
    lateinit var menuReceive: MenuItem
    lateinit var menuSend: MenuItem
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_receive_and_send, menu)

        menuReceive = menu.findItem(R.id.action_receive)
        menuSend = menu.findItem(R.id.action_send)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        when (id) {
            menuReceive.itemId -> showDialogReceive()
            menuSend.itemId -> showDialogSend()
        }

        return super.onOptionsItemSelected(item)
    }


    inner class CheckUserProcess(val billcardNo: String) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg p0: Void?): String {
            val conn = connectionDB.CONN("HR_management")
            val query = "select *  from Sys_SoftwareManagement..sys_UserMenu\n" +
                    " where program_id = '42' and menu_id = '1' and login_id = '$loginID'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                isUserProcess1 = true
            }
            //2
            val query2 = "select *  from Sys_SoftwareManagement..sys_UserMenu\n" +
                    " where program_id = '42' and menu_id = '2' and login_id = '$loginID'"
            val stmt2 = conn.createStatement()
            val rs2 = stmt2.executeQuery(query2)

            while (rs2.next()) {
                isUserProcess2 = true
            }
            //3
            val query3 = "select *  from Sys_SoftwareManagement..sys_UserMenu\n" +
                    " where program_id = '42' and menu_id = '3' and login_id = '$loginID'"
            val stmt3 = conn.createStatement()
            val rs3 = stmt3.executeQuery(query3)

            while (rs3.next()) {
                isUserProcess3 = true
            }
            //4
            val query4 = "select *  from Sys_SoftwareManagement..sys_UserMenu\n" +
                    " where program_id = '42' and menu_id = '4' and login_id = '$loginID'"
            val stmt4 = conn.createStatement()
            val rs4 = stmt4.executeQuery(query4)

            while (rs4.next()) {
                isUserProcess4 = true
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            //check process work
            CheckInProcess(billcardNo).execute()


        }
    }

    inner class GetPartData(val partID: String) : AsyncTask<Unit, Unit, String>() {

        var partNumber = ""
        var relate = ""
        var productDesc = ""
        var productGroup = ""

        override fun onPreExecute() {
            super.onPreExecute()

            dialog = SpotsDialog.Builder()
                    .setContext(this@Detail2Activity)
                    .setMessage("กำลังโหลดข้อมูล...")
                    .setCancelable(false)
                    .build()
                    .apply {
                        show()
                    }
        }

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select  pn.part,pn.relate,pn.description,ld.name from Sys_DataMaster..sys_partnumber_new pn INNER JOIN Sys_DataMaster..sys_partnumber_group pg ON pn.record_id = pg.part_id\n" +
                    "INNER JOIN Sys_DataMaster..layer1_data ld ON pg.group_id = ld.id\n" +
                    "where pn.IsDelete=0 and pg.IsDelete=0 and ld.IsDelete=0  and pn.record_id='$partID'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                partNumber = rs.getString("part")
                relate = rs.getString("relate")
                productDesc = rs.getString("description")
                productGroup = rs.getString("name")
            }
            conn.close()

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            //set value
            txt_part.text = partNumber
            txt_relate.text = relate
            txt_desc.text = productDesc
            txt_group.text = productGroup

            //get part steel
            GetPartSteel(partID).execute()
            //get part composition
            GetPartComposition(partID).execute()
        }
    }

    inner class GetPartSteel(val partId: String?) : AsyncTask<Unit, Unit, String>() {

        private val listSteel: ArrayList<String> = ArrayList()

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select steel_name from Sys_DataMaster..sys_partnumber_steel ps LEFT JOIN Sys_DataMaster..steel s ON ps.steel_id = s.record_id\n" +
                    "where ps.IsDelete=0 and s.IsDelete=0 and ps.part_id = '$partId'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                listSteel.add(rs.getString("steel_name") ?: "")
            }
            conn.close()

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            listSteel.forEach { txt_steel.append(" $it   /") }

        }
    }

    inner class GetPartComposition(val partId: String?) : AsyncTask<Unit, Unit, String>() {

        private var listComposition: ArrayList<String> = ArrayList()

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select composition_name From Sys_DataMaster..sys_partnumber_composition pc LEFT JOIN Sys_DataMaster..composition c ON pc.composition_id=c.composition_id \n" +
                    "where pc.IsDelete = 0 and c.Isdelete=0 and pc.part_id ='$partId'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                listComposition.add(rs.getString("composition_name") ?: "")
            }
            conn.close()

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            listComposition.forEach { txt_composition.append("  $it   /") }
            dialog.dismiss()
        }
    }

    inner class GetCheckProductDesc(val billcardNo: String?) : AsyncTask<Unit, Unit, String>() {

        var count: Int = 0

        var countComplate = ""
        var countCheck = ""
        var productPercent = ""
        var productStatusCheck = ""
        var noteProduct = ""
        var nameCheck = ""
        var dateCheck = ""

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select * from ST_PRODUCTION..check_product where IsDelete = 0 and no_billcard = '$billcardNo'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                count++

                countComplate = rs.getString("qty")
                countCheck = rs.getString("qty_check")
                productPercent = rs.getString("persen_check")
                productStatusCheck = rs.getString("checkproduct")
                noteProduct = rs.getString("remark_check")
                nameCheck = rs.getString("ts_name")
                dateCheck = rs.getString("ts_create")

            }
            conn.close()

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            when (count) {
                0 -> println("no record")
                1 -> {
                    ///set value
                    txt_check_pro_count_complate.text = "จำนวนผลิตเสร็จ  : ${NumberFormat.getNumberInstance(Locale.getDefault()).format(countComplate.toInt())}"
                    txt_check_pro_count_check.text = "จำนวนตรวจสินค้า  : $countCheck"
                    txt_check_pro_percent.text = "เปอร์เซ็นต์ (%)  : $productPercent%"
                    txt_check_pro_status.text = "สินค้า  : $productStatusCheck"
                    txt_check_pro_name.text = "ผู้ตรวจ  : $nameCheck"
                    txt_check_pro_note.text = "หมายเหตุ  : $noteProduct"
                    txt_check_pro_date.text = "วัน/เวลา ตรวจ  : ${convertDataFormat(dateCheck)}"
                }
            }

        }
    }

    inner class GetDataDepartmentDesc(var billcardNo: String?, var orderNo: String?) : AsyncTask<Unit, Unit, String>() {

        var dateProduce = ""
        var datePlan = ""
        var partNumber = ""
        var qty = ""
        var remark = ""
        var dateOnBoard = ""

        override fun doInBackground(vararg params: Unit?): String? {
            val conn = connectionDB.CONN("HR_management")

            val query = "select *,[ST_PRODUCTION].[dbo].[fn_shipingdate]('$orderNo') as date_on_board from\n" +
                    "(select distinct d1.no_billcard,d1.id,d1.part,d1.date,d1.qty,d1.remark,d1.Expr1,d1.sale_order,d2.partnumber,d2.de1,d2.de2,d2.de3,d2.de4\n" +
                    ",(DATEDIFF(day, d1.ts_create,GETDATE()))+1 as daywait,d1.ts_create\n" +
                    "from ST_PRODUCTION..v_BillCardData as d1\n" +
                    "LEFT JOIN ST_PRODUCTION..department_flow as d2 ON d2.id = d1.part\n" +
                    "LEFT JOIN ST_PRODUCTION..production_process_master as d3 ON d3.no_billcard=d1.no_billcard\n" +
                    "where d1.IsDelete='0' and d1.IsReceive='0' and d1.IsProd = '0' and d2.de1='1' and d3.no_billcard IS NULL\n" +
                    "union\n" +
                    "select distinct d1.no_billcard,d1.id,d1.part,d1.date,d1.qty,d1.remark,d1.Expr1,d1.sale_order,d2.partnumber,d2.de1,d2.de2,d2.de3,d2.de4\n" +
                    ",(DATEDIFF(day, d1.ts_create,GETDATE()))+1 as daywait,d1.ts_create\n" +
                    "from ST_PRODUCTION..production_process_master as d1\n" +
                    "LEFT JOIN ST_PRODUCTION..department_flow as d2 ON d2.id = d1.part\n" +
                    "where d1.IsDelete='0' and d1.IsReceive='0' and d1.IsProd = '0' and d2.de1='1') as bill\n" +
                    "where no_billcard LIKE '$billcardNo%'\n" +
                    "order by daywait desc"
            println(query)
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)


            while (rs.next()) {
                dateProduce = rs.getString("date")
                datePlan = rs.getString("ts_create")
                partNumber = rs.getString("partnumber")
                qty = rs.getString("qty")
                remark = rs.getString("remark")
                dateOnBoard = rs.getString("date_on_board") ?: ""
            }
            conn.close()

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            ///set value

            txt_date_plan.text = "วันที่วางแผน  : ${convertDataFormat(datePlan)}"
            txt_order_no.text = "เลขที่ออเดอร์  : $orderNo"

            quantity = NumberFormat.getNumberInstance(Locale.getDefault()).format(Math.round(qty.toFloat()))
            txt_quantity.text = "จำนวน  : $quantity"

            txt_date_produce.text = "วันที่ผลิต  : ${convertOnlyDataFormat(dateProduce)}"
            txt_partnumber.text = "พาร์ทนัมเบอร์  : $partNumber"
            txt_billcard_no.text = "เลขที่บิลการ์ด  : $billcardNo"
            txt_note.text = "หมายเหตุ  : $remark"
            txt_date_shipping.text = "วันที่ลงเรือ : $dateOnBoard"

            //get work day
            GetWorkDay(billcardNo).execute()


        }
    }

    inner class GetWorkDay(val billcardNo: String?) : AsyncTask<Unit, Unit, String>() {
        var dateDiff = ""
        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "SELECT top 1 CASE \n" +
                    "WHEN pp.department=4 or IsComplete = 1 THEN DATEDIFF(day,ppm.date,pp.date_send)+1\n" +
                    "ELSE DATEDIFF(day,ppm.date,GETDATE()+1)\n" +
                    "END date_diff_work\n" +
                    "FROM [ST_PRODUCTION].[dbo].[production_process_master] ppm LEFT JOIN ST_PRODUCTION..production_process pp ON ppm.no_billcard = pp.no_billcard\n" +
                    "where ppm.no_billcard = '$billcardNo'  order by pp.up_update desc"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                dateDiff = rs.getString("date_diff_work")
            }
            conn.close()
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            txt_day_work.text = "วันที่ใช้ทำงาน : $dateDiff  วัน"

        }
    }

    inner class GetFlowWork : AsyncTask<Unit, Unit, String>() {
        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select * FROM [ST_PRODUCTION].[dbo].[department_flow] where id = '$partId'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                dep1 = rs.getString("de1") ?: "null"
                dep2 = rs.getString("de2") ?: "null"
                dep3 = rs.getString("de3") ?: "null"
                dep4 = rs.getString("de4") ?: "null"

            }
            conn.close()


            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            ///set value
            setImageProcess(dep1, img_flow1)
            setImageProcess(dep2, img_flow2)
            setImageProcess(dep3, img_flow3)
            setImageProcess(dep4, img_flow4)

        }
    }

    inner class GetProductProcess(val billcardNo: String?) : AsyncTask<Unit, Unit, String>() {

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select *,DATEDIFF(day,ts_create,up_update)+1 as Date_diff FROM ST_PRODUCTION..production_process where no_billcard = '$billcardNo'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            var dateDiff: String?
            var receive: String?
            var receiveNote: String?
            var receiveDate: String?
            var receiveName: String?
            var send: String?
            var sendNote: String?
            var sendDate: String?
            var sendName: String?
            var department: String?

            while (rs.next()) {

                receive = rs.getString("receive") ?: "0"
                receiveNote = rs.getString("remark_receive") ?: ""
                receiveDate = rs.getString("ts_create") ?: ""
                receiveName = rs.getString("ts_name") ?: ""
                send = rs.getString("send") ?: "0"
                sendNote = rs.getString("remark_send") ?: ""
                sendDate = rs.getString("up_update") ?: ""
                sendName = rs.getString("up_name") ?: ""
                dateDiff = rs.getString("Date_diff") ?: ""

                department = rs.getString("department") ?: ""

                val data = ProductProcessModel(receive, receiveNote, receiveDate, receiveName, send, sendNote, sendDate, sendName, dateDiff, department)
                listProductProcess.add(data)
            }
            conn.close()


            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            listProductProcess.forEach { println(it) }

            for (item in listProductProcess) {
                when (item.department) {
                    "1" -> {
                        recycler_view.layoutManager = LinearLayoutManager(applicationContext)
                        recycler_view.isNestedScrollingEnabled = false
                        recycler_view.adapter = RvDep1Adapter(applicationContext)
                        linear_depart1.visibility = View.VISIBLE
                    }
                    "2" -> {
                        recycler_view_dep2.layoutManager = LinearLayoutManager(applicationContext)
                        recycler_view_dep2.isNestedScrollingEnabled = false
                        recycler_view_dep2.adapter = RvDep2Adapter(applicationContext)
                        linear_depart2.visibility = View.VISIBLE
                    }
                    "3" -> {
                        recycler_view_dep3.layoutManager = LinearLayoutManager(applicationContext)
                        recycler_view_dep3.isNestedScrollingEnabled = false
                        recycler_view_dep3.adapter = RvDep3Adapter(applicationContext)
                        linear_depart3.visibility = View.VISIBLE
                    }
                    "4" -> {
                        recycler_view_dep4.layoutManager = LinearLayoutManager(applicationContext)
                        recycler_view_dep4.isNestedScrollingEnabled = false
                        recycler_view_dep4.adapter = RvDep4Adapter(applicationContext)
                        linear_depart4.visibility = View.VISIBLE
                    }
                    "5" -> {
                        txt_receive_product.text = "จำนวนรับสินค้า  :  ${NumberFormat.getNumberInstance(Locale.getDefault()).format(item.receive.toInt())}"
                        txt_receive_note.text = "หมายเหตุ  :  ${item.receive_note}"
                        txt_recevie_name.text = "ผู้รับ  :  ${item.receive_name}"
                        txt_receive_date.text = "วัน/เวลา รับ  :  ${convertDataFormat(item.receive_date)}"
                    }
                }
            }


        }
    }

    inner class CheckInProcess(val billcardNo: String?) : AsyncTask<Unit, Unit, String>() {
        var process1 = ""
        var process2 = ""
        var process3 = ""
        var process4 = ""
        var isComplete = 0

        override fun doInBackground(vararg params: Unit?): String? {
            val conn = connectionDB.CONN("HR_management")

            //Main DB
            val query = "select TOP 1 *\n" +
                    "  ,([ST_PRODUCTION].[dbo].[fn_production_process](no_billcard,'1')) as '1'\n" +
                    "  ,([ST_PRODUCTION].[dbo].[fn_production_process](no_billcard,'2')) as '2'\n" +
                    "   ,([ST_PRODUCTION].[dbo].[fn_production_process](no_billcard,'3')) as '3'\n" +
                    "   ,([ST_PRODUCTION].[dbo].[fn_production_process](no_billcard,'4')) as '4' \n" +
                    "    from ST_PRODUCTION..production_process\n" +
                    "    where no_billcard = '$billcardNo'"

            //Test DB
//            val query = "select *,d1.status as [1],d2.status as [2],d3.status as [3],d4.status as [4] from ST_PRODUCTION..production_process_master as d0\n" +
//                    "left join (select * from ST_test..production_process where department='1' and IsDelete='0') as d1 ON d1.no_billcard=d0.no_billcard\n" +
//                    "left join (select * from ST_test..production_process where department='2' and IsDelete='0') as d2 ON d2.no_billcard=d0.no_billcard\n" +
//                    "left join (select * from ST_test..production_process where department='3' and IsDelete='0') as d3 ON d3.no_billcard=d0.no_billcard\n" +
//                    "left join (select * from ST_test..production_process where department='4' and IsDelete='0') as d4 ON d4.no_billcard=d0.no_billcard\n" +
//                    "where d0.IsDelete='0' and d0.no_billcard='$billcardNo'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                process1 = rs.getString("1") ?: ""
                process2 = rs.getString("2") ?: ""
                process3 = rs.getString("3") ?: ""
                process4 = rs.getString("4") ?: ""
                isComplete = rs.getInt("isComplete")
            }

            conn.close()
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            //1
            if (isUserProcess1) {
                Log.d("worktag", "user can work: p1 ")

                when {
                    dep1 == "1" && process1 == "" -> {
                        Log.d("detail", "receive1")
                        menuReceive.isVisible = true
                        department = 1
                    }
                    dep1 == "1" && process1 == "0" -> {
                        Log.d("detail", "send1")
                        menuSend.isVisible = true
                        department = 1
                    }
                }

            } else {
                Log.d("worktag", "user cannot work: p1 ")
            }
            //2
            if (isUserProcess2) {
                Log.d("worktag", "user can work: p2 ")

                when {
                    dep2 == "1" && process2 == "" && process1 == "1" -> {

                        if(MainActivity.personal_no == receiveID){
                            Log.d("detail", "receive2")
                            menuReceive.isVisible = true
                            department = 2
                        }
                    }
                    dep2 == "1" && process2 == "0" -> {
                        Log.d("detail", "send2")
                        menuSend.isVisible = true
                        department = 2
                    }
                }

            } else {
                Log.d("worktag", "user cannot work: p2 ")
            }
            //3
            if (isUserProcess3) {
                Log.d("worktag", "user can work: p3 ")
                when {
                    dep3 == "1" && process3 == "" && process2 == "1" -> {
                        Log.d("detail", "receive3")
                        menuReceive.isVisible = true
                        department = 3
                    }
                    dep3 == "1" && process3 == "0" -> {
                        Log.d("detail", "send3")
                        menuSend.isVisible = true
                        department = 3
                    }
                }
            } else {
                Log.d("worktag", "user cannot work: p3 ")
            }
            //4
            if (isUserProcess4) {
                Log.d("worktag", "user can work: p4 ")
                when {
                    dep4 == "1" && process4 == "" && process3 == "1" -> {
                        Log.d("detail", "receive4")
                        menuReceive.isVisible = true
                        department = 4
                    }
                    dep4 == "1" && process4 == "" && process1 == "1" && dep1 == "1" && dep2 == "0" && dep3 == "0" -> {
                        Log.d("detail", "receive4 | 1,4")
                        menuReceive.isVisible = true
                        department = 4
                    }
                    dep4 == "1" && process4 == "0" -> {
                        Log.d("detail", "send4")
                        menuSend.isVisible = true
                        department = 4
                    }
                }

            } else {
                Log.d("worktag", "user cannot work: p4 ")
            }

        }
    }


    inner class GetNameProcess2 : AsyncTask<Unit, Unit, String>() {
        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select data.personnel_no,data.name,pos.position_name,sec.section_name,dep.department_name,dep.department_no FROM [HR_management].[dbo].[pn_datapersonnel] data\n" +
                    "   LEFT JOIN [HR_management].[dbo].[pn_work] pw ON pw.personnel_no = data.personnel_no\n" +
                    "   LEFT JOIN [HR_management].[dbo].[st_position] pos ON pos.position_no = pw.position_no\n" +
                    "   LEFT JOIN  [HR_management].[dbo].[st_section] sec ON pw.section_no = sec.section_no\n" +
                    "   LEFT JOIN [HR_management].[dbo].[st_department] dep ON dep.department_no = pw.department_no\n" +
                    "  where sec.section_no = 'ST01104'\n" +
                    "  and data.delete_bit!=1  and pw.delete_bit!=1 and pos.delete_bit!=1 and sec.delete_bit!=1 and dep.delete_bit!=1\n" +
                    "  order by data.record_id desc"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)

            while (rs.next()) {
                listNameProcess2.add(rs.getString("name"))
                listNameProcess2ID.add(rs.getString("personnel_no"))
            }
            conn.close()


            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)


        }
    }

    var receiveID = ""
    inner class CheckNameReceiveProcess2 : AsyncTask<Unit, Unit, String>() {

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select * from ST_PRODUCTION..production_process where department = 1 and status = 1 and IsDelete = 0 and IsComplete = 0 and no_billcard = '$billCard'"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)


            while (rs.next()) {
                receiveID = rs.getString("personnel_no")
            }
            conn.close()
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }
    }

    inner class InsertReceive(val billcard: String, val partID: String, val saleOrder: String, val receive: String, val department: Int,
                              val remarkReceive: String, val nameWork: String, val personalNo: String) : AsyncTask<Unit, Unit, String>() {
        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val saleOrderStr = when (saleOrder) {
                "" -> "STOCK"
                else -> saleOrder
            }

            val query = "insert into ST_PRODUCTION..production_process(no_billcard,part,sale_order,receive,department,remark_receive,date_receive,status,ts_create,ts_name,IsDelete,personnel_no,IsComplete) \n" +
                    "\tVALUES ('$billcard','$partID','$saleOrderStr',$receive,'$department','$remarkReceive',GETDATE(),0,GETDATE(),'$nameWork',0,'$personalNo',0)"
            val stmt = conn.createStatement()
            stmt.executeUpdate(query)
            conn.close()
            return null
        }

    }

    inner class UpdateSend(val send: String, val remarkSend: String, val upname: String, val selectWorkerID: String, val personnelNo: String, val billcard: String, val department: String) : AsyncTask<Unit, Unit, String>() {
        override fun doInBackground(vararg p0: Unit?): String {
            val conn = connectionDB.CONN("HR_management")

            val query = "update ST_PRODUCTION..production_process set send = $send,remark_send='$remarkSend',date_send = GETDATE(),status=1,up_update = GETDATE(),up_name='$upname',personnel_no='$selectWorkerID',personnel_work = '$personnelNo' where no_billcard = '$billcard' and department = '$department'"
            val stmt = conn.createStatement()
            stmt.executeUpdate(query)
            conn.close()
            return ""
        }
    }

    inner class InsertBillcardToMaster : AsyncTask<Unit,Unit,String>(){
        override fun doInBackground(vararg p0: Unit?): String {
            val conn = connectionDB.CONN("ST_PRODUCTION")

            val query = """
                            INSERT INTO ST_PRODUCTION..production_process_master (id
                  ,no_billcard
                  ,part
                  ,type_id
                  ,unit_id
                  ,so_id
                  ,date
                  ,qty
                  ,qty_prod
                  ,detail_ref
                  ,tran_ref
                  ,dating_ref
                  ,ts_create
                  ,ts_name
                  ,IsDelete
                  ,IsProd
                  ,IsReceive
                  ,remark
                  ,prod_remark
                  ,record_id
                  ,Expr1
                  ,relate
                  ,type
                  ,brand_id
                  ,description
                  ,sale_order)
            SELECT id
                  ,no_billcard
                  ,part
                  ,type_id
                  ,unit_id
                  ,so_id
                  ,date
                  ,qty
                  ,qty_prod
                  ,detail_ref
                  ,tran_ref
                  ,dating_ref
                  ,ts_create
                  ,ts_name
                  ,IsDelete
                  ,IsProd
                  ,IsReceive
                  ,remark
                  ,prod_remark
                  ,record_id
                  ,Expr1
                  ,relate
                  ,type
                  ,brand_id
                  ,description
                  ,sale_order
            FROM ST_PRODUCTION..v_BillCardData
            WHERE id = $id
            """.trimIndent()

            val stmt = conn.createStatement()
            stmt.executeUpdate(query)
            
            conn.close()
            return ""
        }
    }

    inner class UpdateSendIsComplete(val billcard: String) : AsyncTask<Unit, Unit, String>() {
        override fun doInBackground(vararg p0: Unit?): String {
            val conn = connectionDB.CONN("ST_PRODUCTION")

            val query = "update ST_PRODUCTION..production_process set IsComplete = 1,ts_complete = GETDATE() where no_billcard = '$billcard'"
            val stmt = conn.createStatement()
            stmt.executeUpdate(query)

            conn.close()
            return ""
        }
    }

    private fun loadImageProduct(HTTP: String) {
        val stringRequest = StringRequest(Request.Method.POST, HTTP, Response.Listener { s ->
            val array = JSONArray(s)
            when (array.length()) {
                0 -> image_product.setImageDrawable(resources.getDrawable(R.drawable.no_image, null))
                else -> {
                    val objectProduct = array.getJSONObject(0)
                    val img = objectProduct.getString("image")
                    val imgURL = "http://roomdatasoftware.strubberdata.com/pic_partnumber_new/$img"
                    Glide.with(this).load(imgURL).into(image_product)
                }
            }

        }, Response.ErrorListener { e -> Toast.makeText(applicationContext, "error : ${e.message}", Toast.LENGTH_SHORT).show() })

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    private fun convertDataFormat(strDate: String): String {
        var dateStr: String? = strDate
        val result = when (dateStr) {
            "" -> return ""
            null -> println("null")
            else -> {
                val curFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                var dateObj: Date?
                dateObj = curFormat.parse(dateStr)
                val newFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
                return newFormat.format(dateObj)
            }
        }
        return result.toString()
    }

    private fun convertOnlyDataFormat(strDate: String): String {
        var dateStr: String? = strDate
        val result = when (dateStr) {
            "" -> return ""
            null -> println("null")
            else -> {
                val curFormat = SimpleDateFormat("yyyy-MM-dd")
                var dateObj: Date?
                dateObj = curFormat.parse(dateStr)
                val newFormat = SimpleDateFormat("dd/MM/yyyy")
                return newFormat.format(dateObj)
            }
        }
        return result.toString()
    }

    private fun setImageProcess(dep: String?, img: ImageView) {
        when (dep) {
            "1" -> img.setImageDrawable(resources.getDrawable(R.drawable.checked, null))
            "0" -> img.setImageDrawable(resources.getDrawable(R.drawable.cancel, null))
        }
    }

    private fun getLastQuantity(): String? {

        var productBalance: String? = null
        if (listProductProcess.size >= 1) {
            productBalance = when (listProductProcess[0].send.toInt()) {
                0 -> listProductProcess[0].receive
                else -> listProductProcess[0].send
            }
        }
        if (listProductProcess.size >= 2) {
            productBalance = when (listProductProcess[1].send.toInt()) {
                0 -> listProductProcess[1].receive
                else -> listProductProcess[1].send
            }
        }
        if (listProductProcess.size >= 3) {
            productBalance = when (listProductProcess[2].send.toInt()) {
                0 -> listProductProcess[2].receive
                else -> listProductProcess[2].send
            }
        }
        if (listProductProcess.size >= 4) {
            productBalance = when (listProductProcess[3].send.toInt()) {
                0 -> listProductProcess[3].receive
                else -> listProductProcess[3].send
            }
        }
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(productBalance?.toInt())
    }

    private fun setValueLostProduct(holder: ViewHolder, index: Int) {

        ///get last quantity
        txt_balance_product.text = "คงเหลือ = ${getLastQuantity()}"

        var lostWork = "0"
        var lostSend = "0"
        //get lose in work
        if (listProductProcess[index].send.toInt() != 0 && listProductProcess[index].receive.toInt() != 0) {
            val var1 = listProductProcess[index].send.toInt()
            val var2 = listProductProcess[index].receive.toInt()
            lostWork = var1.minus(var2).toString()
        }
        //get lose in send work
        if (listProductProcess.size > index + 1) {
            if (listProductProcess[index].send.toInt() != 0 && listProductProcess[index + 1].receive.toInt() != 0) {

                val var1 = listProductProcess[index].send.toInt()
                val var2 = listProductProcess[index + 1].receive.toInt()
                lostSend = var2.minus(var1).toString()
            }
        }

        holder.txtLost.text = "$lostWork | $lostSend"

        if (lostWork.toInt() < 0 || lostSend.toInt() < 0)
            holder.txtLost.setTextColor(Color.parseColor("#E74955"))
        else if (lostWork.toInt() > 0 || lostSend.toInt() > 0) {
            holder.txtLost.setTextColor(Color.parseColor("#7AA410"))
        }
        //holder.txt_lost.typeface = Typeface.DEFAULT_BOLD
    }

    private fun showDialogReceive() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = layoutInflater
        val view = inflater.inflate(R.layout.custom_dialog_receive, null)
        builder.setView(view)

        val edtReceive: EditText = view.findViewById(R.id.edtReceive)
        val edtRemarkReceive: EditText = view.findViewById(R.id.edtRemarkReceive)
        edtReceive.setText(quantity)

        builder.setPositiveButton("บันทึก") { _, _ -> }
        builder.setNegativeButton("ยกเลิก") { _, _ -> }

        val alertDialog = builder.create()

        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

            if (edtReceive.text.isEmpty()) {
                edtReceive.error = "ใส่จำนวนรับด้วย"
                edtReceive.requestFocus()
            } else {

                //insert receive
                InsertReceive(billCard, partId, orderNo, edtReceive.text.toString(), department, edtRemarkReceive.text.toString(), MainActivity.name, MainActivity.personal_no).execute()

                //insert billcard to master
                InsertBillcardToMaster().execute()

                alertDialog.dismiss()
                finish()
            }
        }
    }


    private fun showDialogSend() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = layoutInflater
        val view = inflater.inflate(R.layout.custom_dialog_send, null)
        builder.setView(view)

        val edtReceive: EditText = view.findViewById(R.id.edtReceive)
        val edtRemarkReceive: EditText = view.findViewById(R.id.edtRemarkReceive)
        val edtSend: EditText = view.findViewById(R.id.edtSend)
        val edtRemarkSend: EditText = view.findViewById(R.id.edtRemarkSend)

        val linearSpinner: LinearLayout = view.findViewById(R.id.linear_spinner)

        if (department == 1) {
            linearSpinner.visibility = View.VISIBLE
        }

        edtReceive.setText(listProductProcess[0].receive)
        edtRemarkReceive.setText(listProductProcess[0].receive_note)
        edtSend.requestFocus()
        edtSend.setText(listProductProcess[0].receive)


        //Get name dep2
        listNameProcess2.clear()
        listNameProcess2ID.clear()
        listNameProcess2.add(0, "เลือกพนักงาน")
        listNameProcess2ID.add(0, "")
        GetNameProcess2().execute()

        var workerID = ""
        val spinner: Spinner = view.findViewById(R.id.spinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listNameProcess2)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                Toast.makeText(applicationContext, "select:${listNameProcess2[p2]} ID :${listNameProcess2ID[p2]}", Toast.LENGTH_SHORT).show()
                workerID = listNameProcess2ID[p2]
            }
        }
        //end


        ///Check IsComplete
        val depAll = "$dep1$dep2$dep3$dep4"
        Log.d("detail", depAll)

//        1111    1001    1101    0001    0101    0011  -> finish4
//        1110    1010    0010    0110                  -> finish3
//        1100    0100                                  -> finish2
//        1000                                          -> finish1
        val strFinish = when (depAll) {
            "1111" -> 4
            "1001" -> 4
            "1101" -> 4
            "0001" -> 4
            "0101" -> 4
            "0011" -> 4
            "1110" -> 3
            "1010" -> 3
            "0010" -> 3
            "0110" -> 3
            "1100" -> 2
            "0100" -> 2
            "1000" -> 1
            else -> {
            }
        }
        //end

        builder.setPositiveButton("บันทึก") { _, _ -> }
        builder.setNegativeButton("ยกเลิก") { _, _ -> }

        val alertDialog = builder.create()

        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            when {
                edtSend.text.isEmpty() -> {
                    edtSend.error = "ใส่จำนวนส่งด้วย"
                    edtSend.requestFocus()
                }
                workerID == "" && linearSpinner.visibility == View.VISIBLE -> {

                    val errorText: TextView = spinner.selectedView as TextView
                    errorText.error = "is error"
                    errorText.setTextColor(Color.RED)
                    errorText.text = "กรุณาเลือก พนักงาน"
                }
                else -> {

                    UpdateSend(edtSend.text.toString(), edtRemarkSend.text.toString(), MainActivity.name, workerID, MainActivity.loginId, billCard, department.toString()).execute()
                    if (department == strFinish) {
                        Log.d("detail", "this dep $department IsComplete")
                        UpdateSendIsComplete(billCard).execute()
                    } else {
                        Log.d("detail", "this dep $department Not IsComplete")
                    }



                    alertDialog.dismiss()
                    finish()
                }

            }
        }
    }


    inner class RvDep1Adapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

        // Gets the number of animals in the list
        override fun getItemCount(): Int = 1

        // Inflates the item views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@Detail2Activity).inflate(R.layout.item_process_product, parent, false))
        }

        // Binds each animal in the ArrayList to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            if (listProductProcess.size >= 1) {
                for (item in listProductProcess) {
                    if (item.department == "1") {
                        holder.txtReceive.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.receive.toInt())
                        holder.txtReceiveNote.text = item.receive_note
                        holder.txtReceiveDate.text = convertDataFormat(item.receive_date)
                        holder.txtReceiveName.text = item.receive_name
                        holder.txtSend.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.send.toInt())
                        holder.txtSendNote.text = item.send_note
                        holder.txtSendDate.text = convertDataFormat(item.send_date)
                        holder.txtSendName.text = item.send_name
                        holder.txtDayWork.text = "${item.date_diff}  วัน"
                    }
                }
                setValueLostProduct(holder, 0)
            }


        }
    }

    inner class RvDep2Adapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return 1
        }

        // Inflates the item views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@Detail2Activity).inflate(R.layout.item_process_product, parent, false))
        }

        // Binds each animal in the ArrayList to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            if (listProductProcess.size >= 2) {

                setValueLostProduct(holder, 1)
            }

            for (item in listProductProcess) {
                if (item.department == "2") {
                    holder.txtReceive.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.receive.toInt())
                    holder.txtReceiveNote.text = item.receive_note
                    holder.txtReceiveDate.text = convertDataFormat(item.receive_date)
                    holder.txtReceiveName.text = item.receive_name
                    holder.txtSend.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.send.toInt())
                    holder.txtSendNote.text = item.send_note
                    holder.txtSendDate.text = convertDataFormat(item.send_date)
                    holder.txtSendName.text = item.send_name
                    holder.txtDayWork.text = "${item.date_diff}  วัน"
                }
            }
        }
    }

    inner class RvDep3Adapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return 1
        }

        // Inflates the item views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@Detail2Activity).inflate(R.layout.item_process_product, parent, false))
        }

        // Binds each animal in the ArrayList to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (listProductProcess.size >= 3) {
                setValueLostProduct(holder, 2)
            }

            for (item in listProductProcess) {
                if (item.department == "3") {
                    holder.txtReceive.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.receive.toInt())
                    holder.txtReceiveNote.text = item.receive_note
                    holder.txtReceiveDate.text = convertDataFormat(item.receive_date)
                    holder.txtReceiveName.text = item.receive_name
                    holder.txtSend.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.send.toInt())
                    holder.txtSendNote.text = item.send_note
                    holder.txtSendDate.text = convertDataFormat(item.send_date)
                    holder.txtSendName.text = item.send_name
                    holder.txtDayWork.text = "${item.date_diff}  วัน"
                }
            }
        }
    }

    inner class RvDep4Adapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return 1
        }

        // Inflates the item views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@Detail2Activity).inflate(R.layout.item_process_product, parent, false))
        }

        // Binds each animal in the ArrayList to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (listProductProcess.size >= 4) {
                setValueLostProduct(holder, 3)
            }

            for (item in listProductProcess) {
                if (item.department == "4") {
                    holder.txtReceive.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.receive.toInt())
                    holder.txtReceiveNote.text = item.receive_note
                    holder.txtReceiveDate.text = convertDataFormat(item.receive_date)
                    holder.txtReceiveName.text = item.receive_name
                    holder.txtSend.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.send.toInt())
                    holder.txtSendNote.text = item.send_note
                    holder.txtSendDate.text = convertDataFormat(item.send_date)
                    holder.txtSendName.text = item.send_name
                    holder.txtDayWork.text = "${item.date_diff}  วัน"
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtReceive: TextView = view.txt_receive
        val txtReceiveNote: TextView = view.txt_receive_note
        val txtReceiveDate: TextView = view.txt_receive_date
        val txtReceiveName: TextView = view.txt_receive_name
        val txtSend: TextView = view.txt_send
        val txtSendNote: TextView = view.txt_send_note
        val txtSendDate: TextView = view.txt_send_date
        val txtSendName: TextView = view.txt_send_name
        val txtDayWork: TextView = view.txt_day_work
        val txtLost: TextView = view.txt_lost

    }

}

