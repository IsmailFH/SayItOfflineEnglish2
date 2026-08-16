package com.sayit.offlineenglish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent

class ProductActivity : ComponentActivity() {
    private val vm: EnglishCoachViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SayItProductApp(vm) }
    }
}
