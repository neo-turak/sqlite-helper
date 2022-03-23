package cn.nurasoft.jupiter

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import cn.nurasoft.sqlite.*

class LocalHelper(ctx: Context) : SQLiteMagicHelper(ctx, "MyDb",null,1) {
    init {
        instance = this
    }
    companion object {
        private var instance: LocalHelper? = null

        @Synchronized
        fun getInstance(ctx: Context) = instance ?: LocalHelper(ctx.applicationContext)
    }
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.createTable("user",true,
            "id" to INTEGER + PRIMARY_KEY + UNIQUE,
            "name" to TEXT,
            "gender" to TEXT
            )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.dropTable("user",true)
    }

}

// Access property for Context
val Context.database: LocalHelper
    get() = LocalHelper.getInstance(this)

