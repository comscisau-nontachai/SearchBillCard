package dev.strubber.com.searchbillcard

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class SCRActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scr)

        startActivity(Intent(applicationContext,LoginActivity::class.java))
        finish()
    }
}
