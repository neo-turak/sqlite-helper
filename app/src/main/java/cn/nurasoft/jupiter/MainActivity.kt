package cn.nurasoft.jupiter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import cn.nurasoft.sqlite.classParser
import cn.nurasoft.sqlite.insert
import cn.nurasoft.sqlite.select
import cn.nurasoft.jupiter.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var _binding:ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding= ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(_binding.root)
        _binding.hWorld.setOnClickListener {
            val random= Random.nextInt(1000)
            this.database.use {
                this.insert("hola",
                "name" to  random.toString(),
                    "gender" to random.toString())
            }
        }
        _binding.btnGet.setOnClickListener {
            this.database.use {
                val parser= classParser<Person>()
             val  list= this.select("hola").parseList(parser)
                _binding.tvContent.text = list.map { it.name }.joinToString { it+"\n" }
            }
        }
    }
}