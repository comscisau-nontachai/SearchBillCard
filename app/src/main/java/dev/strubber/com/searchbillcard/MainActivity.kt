package dev.strubber.com.searchbillcard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_by_search.view.*
import okhttp3.internal.Util
import org.json.JSONArray
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var anim_down: Animation
    private lateinit var anim_up: Animation

    lateinit var billcard_no: String
    lateinit var partnumber: String
    lateinit var order_no: String
    lateinit var part_id: String

    val TAG = "maintag"

    ///connnection DB
    private val connectionDB = ConnectionDB()

    val listSearch: ArrayList<SearchModel> = ArrayList()

    ///User Data
    data class SearchModel(var billcard_no: String, var partnumber: String, var order_no: String, var part_id: String)

    companion object {
        var loginId = ""
        var loginPersonal = ""
        var personal_no = ""
        var name = ""
    }


    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ///set anime search bar
        anim_down = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        anim_up = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        img_logo.animation = anim_down
        searchView.animation = anim_up

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                listSearch.clear()
                recycler_search.visibility = View.VISIBLE
                SearchTask(query).execute()

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                when (newText?.length) {
                    0 -> {
                        recycler_search.visibility = View.GONE
                    }
                }

                return false
            }
        })

        ///Search from Barcode
        btn_search.setOnClickListener {
//            val intent = Intent(this,OCRActivity::class.java)
            val intent = Intent(this,BarcodeActivity::class.java)
            startActivity(intent)

        }
        //get userData
        loginId = intent.getStringExtra("LOGIN_ID")
        loginPersonal = intent.getStringExtra("LOGIN_PERSONAL")

        GetUserData(loginId,loginPersonal).execute()

        img_exit.setOnClickListener { finish() }
    }

    inner class RvAdapter(private val items: ArrayList<SearchModel>, private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return items.size
        }

        // Inflates the item views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.item_by_search, parent, false))
        }

        // Binds each animal in the ArrayList to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tv_billcard.text = items[position].billcard_no
            holder.tv_partnumber.text = items[position].partnumber
            holder.tv_order_no.text = items[position].order_no

            holder.linear_holder.setOnClickListener {
                val intent = Intent(applicationContext, Detail2Activity::class.java)
                intent.putExtra("BILLCARD_NO", listSearch[position].billcard_no)
                intent.putExtra("PARTNUMBER", listSearch[position].partnumber)
                intent.putExtra("ORDER_NO", listSearch[position].order_no)
                intent.putExtra("PART_ID", listSearch[position].part_id)
                startActivity(intent)
            }
        }
    }
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tv_billcard: TextView = view.txt_billcard_no
        val tv_partnumber: TextView = view.txt_partnumber
        val tv_order_no: TextView = view.txt_order_no
        val linear_holder: LinearLayout = view.linear_holder

    }

    inner class SearchTask(private val str: String?) : AsyncTask<Unit, Unit, String>() {

        lateinit var dialog : android.app.AlertDialog

        override fun onPreExecute() {
            super.onPreExecute()

            dialog = SpotsDialog.Builder()
                    .setContext(this@MainActivity)
                    .setMessage("กำลังหาข้อมูล...")
                    .setCancelable(false)
                    .build()
                    .apply {
                        show()
                    }
        }

        override fun doInBackground(vararg params: Unit?): String? {

            val conn = connectionDB.CONN("HR_management")

            val query = "select * from\n" +
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
                    "where d1.IsDelete='0' and d1.IsReceive='0' and d1.IsProd = '0' and d2.de1='1' ) as bill\n" +
                    "where no_billcard LIKE '%$str%'\n" +
                    "order by daywait desc\n"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(query)


            while (rs.next()) {
                billcard_no = rs.getString("no_billcard")
                partnumber = rs.getString("Expr1")
                order_no = rs.getString("sale_order")?:""
                part_id = rs.getString("part")

                val data = SearchModel(billcard_no, partnumber, order_no, part_id)
                listSearch.add(data)
            }
            conn.close()

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            dialog.dismiss()
            recycler_search.layoutManager = LinearLayoutManager(applicationContext)
            recycler_search.adapter = RvAdapter(listSearch, applicationContext)
        }
    }

    private inner class GetUserData(val u_login_id:String,val u_login_personel:String):AsyncTask<Void,Void,String>(){

        lateinit var dialog: SpotsDialog



        override fun onPreExecute() {
            super.onPreExecute()

            dialog = SpotsDialog.Builder()
                    .setContext(this@MainActivity)
                    .setMessage("กำลังเข้าสู่ระบบ...")
                    .setCancelable(false)
                    .build().apply { show() } as SpotsDialog

        }
        override fun doInBackground(vararg params: Void?): String? {
            
            val conn = connectionDB.CONN("HR_management")
            if(conn!=null){
                val query = "select * from HR_management..pn_datapersonnel where login_id='$u_login_id' or personnel_no='$u_login_personel' and delete_bit=0"
                val stmt =conn.createStatement()
                val rs = stmt.executeQuery(query)
                while (rs.next()){
                    personal_no = rs.getString("personnel_no")
                    name = rs.getString("name")
                }
            }else
            {
                Toast.makeText(this@MainActivity, "can't connect to server !!, please contact admin.", Toast.LENGTH_SHORT).show()
            }
            conn.close()

            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            dialog.dismiss()

            txt_name.text = name
            val url = "http://hrmsoftware.strubberdata.com/personnel_img/getImagePersonal.php?id=$personal_no"
            getImagePerson(url)
        }
    }

    private fun getImagePerson(url:String){
        var image = ""
        val request = JsonArrayRequest(Request.Method.GET,url,null,
                Response.Listener {
                    for (i in 0 until it.length()){
                        val jsonObject = it.getJSONObject(i)
                        image = "http://hrmsoftware.strubberdata.com/personnel_img/${jsonObject.getString("image")}"
                    }
                    val options = RequestOptions().centerCrop().error(R.drawable.no_image).placeholder(R.drawable.no_image)
                    Glide.with(this).load(image).apply(options).into(image_profile)
                },
                Response.ErrorListener { Toast.makeText(this, "can't get image. ${it.message}", Toast.LENGTH_SHORT).show(); })
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(request)
    }

//    private fun clearSearchText(){
//        listSearch.clear()
//        searchView.text
//    }

}

