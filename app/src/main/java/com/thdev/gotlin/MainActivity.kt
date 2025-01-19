
package com.thdev.gotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.thdev.gotlin.databinding.ActivityMainBinding

public class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    
    private val binding: ActivityMainBinding
      get() = checkNotNull(binding) { "destroyed" }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
