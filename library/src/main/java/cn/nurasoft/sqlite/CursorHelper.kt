package cn.nurasoft.sqlite
import android.database.Cursor

object CursorHelper {

    @JvmStatic
    inline fun <T> useCursor(cursor: Cursor, f: (Cursor) -> T): T {
        return cursor.use(f)
    }

}
