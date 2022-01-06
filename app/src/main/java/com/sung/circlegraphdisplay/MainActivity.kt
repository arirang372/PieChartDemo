package com.sung.circlegraphdisplay

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), CircleDisplay.SelectionListener {
    private lateinit var mCircleDisplay: CircleDisplay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCircleDisplay = findViewById(R.id.circleDisplay)
        mCircleDisplay.setAnimDuration(4000)
        mCircleDisplay.setValueWidthPercent(55f)
        mCircleDisplay.setFormatDigits(1)
        mCircleDisplay.setDimAlpha(80)
        mCircleDisplay.setSelectionListener(this)
        mCircleDisplay.setTouchEnabled(true)
        mCircleDisplay.setUnit("%")
        mCircleDisplay.setStepSize(0.5f)
        mCircleDisplay.showValue(75f, 100f, true)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.refresh) mCircleDisplay.showValue((Math.random() * 1000f).toFloat(),
            1000f,
            true)
        return true
    }

    override fun onSelectionUpdate(value: Float, maxval: Float) {
        Log.i("Main", "Selection update: $value, max: $maxval")
    }

    override fun onValueSelected(value: Float, maxval: Float) {
        Log.i("Main", "Selection complete: $value, max: $maxval")
    }
}