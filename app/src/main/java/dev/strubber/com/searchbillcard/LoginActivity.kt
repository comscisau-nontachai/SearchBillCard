package dev.strubber.com.searchbillcard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.Preference
import android.util.Log
import android.widget.Toast
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import dev.strubber.com.searchbillcard.R.id.*
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONArray

class LoginActivity : AppCompatActivity() {

    lateinit var login_id: String
    lateinit var login_personnal: String


    lateinit var loginPreferences: SharedPreferences
    lateinit var loginPrefsEditor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        loginPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        loginPrefsEditor = loginPreferences.edit()

        btn_login.setOnClickListener {

            callLogin()

            ///save user\pass
            val username = edt_username.text.toString()
            val password = edt_password.text.toString()
            if (checkbox_remem.isChecked) {
                loginPrefsEditor.apply {
                    putBoolean("saveLogin", true)
                    putString("username", username)
                    putString("password", password)
                    loginPrefsEditor.commit()
                }
            } else {
                loginPrefsEditor.clear()
                loginPrefsEditor.commit()
            }
        }

        val saveLogin = loginPreferences.getBoolean("saveLogin", false)
        if (saveLogin) {
            edt_username.setText(loginPreferences.getString("username", ""))
            edt_password.setText(loginPreferences.getString("password", ""))
            checkbox_remem.isChecked = true
        }


        ///auto login
        if(edt_username.text.isNotEmpty()){
            callLogin()
        }


    }

    private fun callLogin() {
        val username = edt_username.text.toString().trim()
        val password = edt_password.text.toString().trim()

        if (username.isEmpty() && password.isEmpty()) {
            val pDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            pDialog.titleText = "ใส่ username | password ด้วย..."
            pDialog.confirmText = "ตกลง"
            pDialog.setConfirmClickListener {
                it.dismissWithAnimation()
            }
            pDialog.show()
        } else {
            val HTTP_URL = "http://hrmsoftware.strubberdata.com/personnel_img/check_login.php?user=$username&pass=$password"
            val request = StringRequest(Request.Method.GET, HTTP_URL,
                    Response.Listener {
                        if (it.contains("[]")) {

                            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).apply {
                                titleText = "Username/Password ไม่ถูกต้อง...\nกรุณาติดต่อเจ้าหน้าที่"
                                setConfirmButton("ตกลง", SweetAlertDialog.OnSweetClickListener { it.dismissWithAnimation() })
                                show()
                            }

                        } else {
                            LoginAsyn(it).execute()
                        }
                    },
                    Response.ErrorListener { Toast.makeText(this, "can't connect!!", Toast.LENGTH_SHORT).show(); })
            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(request)
        }
    }

    private inner class LoginAsyn(val it: String) : AsyncTask<Void, Void, String>() {

        lateinit var dialog: SpotsDialog
        override fun onPreExecute() {
            super.onPreExecute()

            dialog = SpotsDialog.Builder()
                    .setContext(this@LoginActivity)
                    .setMessage("กำลังเข้าสู่ระบบ...")
                    .setCancelable(false)
                    .build().apply { show() } as SpotsDialog

        }

        override fun doInBackground(vararg params: Void?): String? {

            val jsonArray = JSONArray(it)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                login_id = jsonObject.getString("login_id")
                login_personnal = jsonObject.getString("login_personnal")
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            dialog.dismiss()
            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                putExtra("LOGIN_ID", login_id)
                putExtra("LOGIN_PERSONAL", login_personnal)
            }
            startActivity(intent)
        }


    }
}
