# Android SQLite 神器 - Anko

你是否厌倦了使用Android游标解析SQLite查询结果？为了解析查询结果行，你必须编写大量样板代码，然后把它包含在无数个 `try..finally` 模块中并且适当的时候关闭全部资源.

Anko提供了许多扩展函数来简化使用SQLite数据库的工作。

## 目录

* [在项目中使用Anko SQLite](#在项目中使用Anko-SQLite)
* [访问数据库](#访问数据库)
* [创建和删除表](#创建和删除表)
* [插入数据](#插入数据)
* [查询数据](#查询数据)
* [删除数据](#删除数据)
* [解析查询结果](#解析查询结果)
* [自定义行解析器](#自定义行解析器)
* [游标流](#游标流)
* [更新数据](#更新数据)
* [事务处理](#事务处理)


## 在项目中使用Anko SQLite

添加 `anko-sqlite` 依赖到 `build.gradle`:

[![](https://jitpack.io/v/Neo-Turak/sqlite-helper.svg)](https://jitpack.io/#Neo-Turak/sqlite-helper)

```groovy
dependencies {
    implementation 'com.github.Neo-Turak:sqlite-helper:v1.0.0'
}
```

## 访问数据库

如果使用 `SQLiteOpenHelper`，通常会调用 `getReadableDatabase()`或`getWritableDatabase()`（结果在生产代码中实际上是相同的），但必须确保对收到的`SQLiteDatabase`调用`close()`方法。此外，必须将helper类缓存在某个位置，如果从多个线程使用它，则必须知道并发访问。这一切都很艰难。这就是为什么Android开发者并不真正热衷于默认的SQLite API，而是更喜欢使用相当昂贵的包装器，比如ORMs。

Anko提供了一个特殊的类`SQLiteMagicHelper`，可以无缝地替换默认类。以下是你如何使用它:

```kotlin
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
        //创建表，
        db!!.createTable("user",true,
            "id" to INTEGER + PRIMARY_KEY + UNIQUE,
            "name" to TEXT,
            "gender" to TEXT
            )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //通常一样 升级表
        db!!.dropTable("user",true)
    }

}

// 上下文的访问
val Context.database: LocalHelper
    get() = LocalHelper.getInstance(this)
```

那是什么意思？现在，您可以编写以下代码，而不是将代码封装到“try”块中：

```kotlin
database.use {
    // `this` 是个 SQLiteDatabase 实例
}
```

数据库在执行 `{}`内所有代码后自动关闭.

异步调用示例:

```kotlin
class SomeActivity : Activity() {
    private fun loadAsync() {
        async(UI) {
            val result = bg { 
                database.use { ... }
            }
            loadComplete(result)
        }
    }
}
```

<table>
<tr><td width="50px" align="center"></td>
<td>
<i>下面提到的这些方法和所有方法可能会触发 <code>SQLiteException</code>异常.这个你必须得自己处理，Anko认为假装没有发生错误是不合理的。</i>
</td>
</tr>
</table>

## 创建和删除表

使用Anko，您可以轻松创建新表并删除现有表。语法很简单.

```kotlin
database.use {
    createTable("Customer", true, 
        "id" to INTEGER + PRIMARY_KEY + UNIQUE,
        "name" to TEXT,
        "photo" to BLOB)
}
```

在SQLite中，有五种主要类型：`NULL`、`INTEGER`、`REAL`、`TEXT`和`BLOB`。但每一列可能都有一些修饰符，如`主键`或`唯一键`。您可以通过将这些修饰符`添加`到主类型名称中来附加这些修饰符.

要删除表格，请使用`dropTable`功能:

```kotlin
dropTable("User", true)
```

## 插入数据

通常，您需要一个`ContentValues`实例来向表中插入一行。下面是一个例子:

```kotlin
val values = ContentValues()
values.put("id", 5)
values.put("name", "John Smith")
values.put("email", "user@domain.org")
db.insert("User", null, values)
```

Anko允许您通过直接将值作为`insert()`函数的参数传递来消除这种客套操作:

```kotlin
//  db 是 SQLiteDatabase
// 例: val db = database.writeableDatabase
db.insert("User", 
    "id" to 42,
    "name" to "John",
    "email" to "user@domain.org"
)
```

或者在 `database.use` 内:

```kotlin
database.use {
    insert("User", 
        "id" to 42,
        "name" to "John",
        "email" to "user@domain.org"
   )
}
```
请注意，在上面的示例中，`database`是一个数据库助手实例，`db`是一个`SQLiteDatabase`对象

函数 `insertOrThrow()`, `replace()`, `replaceOrThrow()` 也有相同的语义。

## 查询数据

Anko 提供了很方便的查询生成器。它可以用`db.select(tableName,vararg columns)`来创建，这里的`db`是`SQLiteDatabase`实例。

方法                                  | 描述
--------------------------------------|---------- 
`column(String)`                      | 为查询添加一列
`distinct(Boolean)`                   | 去重查询
`whereArgs(String)`                   | 用 `where` 条件查询
`whereArgs(String, args)` :star:      | 用 `where` 和条件参数进行查询
`whereSimple(String, args)`           | 用 `where` 加 `?` 作为查询条件
`orderBy(String, [ASC/DESC])`         | 按此列排序
`groupBy(String)`                     | 按此列分组
`limit(count: Int)`                   | 限制查询结果总行数
`limit(offset: Int, count: Int)`      | 使用偏移量限制查询结果行数
`having(String)`                      | 指定原始`having` 表达式
`having(String, args)` :star:         | 指定 `having` 表达式跟参数

标记为：star的函数：以特殊方式解析其参数。它们允许您以任何顺序提供值，并支持无缝转义.

```kotlin
db.select("User", "name")
    .whereArgs("(_id > {userId}) and (name = {userName})",
        "userName" to "John",
        "userId" to 42)
```

在这里，`{userId}`部分将被替换为`42`，而`{userName}`部分将被替换为`John`。如果值的类型不是数值（`Int`、`Float`等）或`Boolean`，则该值将被转义。对于任何其他类型，将使用`toString()`表示。

`whereSimple`函数接受`String`类型的参数。它的工作原理与[`query()`]相同(http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#query（java.lang.String、%20java.lang.String[]、%20java.lang.String、%20java.lang.String[]、%20java.lang.String、%20java.lang.String、%20java.lang.String、%20java.lang.String)）来自`SQLiteDatabase`（问号`？`将被替换为参数中的实际值）。

我们如何执行查询？使用`exec()`函数。它接受`游标类型的扩展函数。()->T`。它只需启动接收到的扩展函数，然后关闭`Cursor`，这样您就不需要自己操作了：

```kotlin
db.select("User", "email").exec {
	// 做操作
}
```

## 解析查询结果

所以我们有一些`Cursor`，我们如何将其解析为常规类？Anko提供了`parseSingle`、`parseOpt`和`parseList`函数，可以更轻松地完成这项工作。

方法                                  | 说明
--------------------------------------|---------- 
`parseSingle(rowParser): T`           | 只解析一行
`parseOpt(rowParser): T?`             | 解析零行或一行
`parseList(rowParser): List<T>`       | 解析零行或多行

请注意，如果收到的游标包含多行，则`parseSingle()`和`parseOpt()`将出现异常。

现在的问题是：`rowParser`是什么？每个函数都支持两种不同类型的解析器：`RowParser`和`MapRowParser`：

```kotlin
interface RowParser<T> {
    fun parseRow(columns: Array<Any>): T
}

interface MapRowParser<T> {
    fun parseRow(columns: Map<String, Any>): T
}
```

如果您想以非常高效的方式编写查询，请使用RowParser（但是您必须知道每列的索引）`parseRow'接受'Any'的列表（Any'的类型实际上只能是'Long'、'Double'、'String'或'ByteArray'）`另一方面，MapRowParser允许您使用列名获取行值。

Anko已经有了用于简单单列行的解析器：

* `ShortParser`
* `IntParser`
* `LongParser`
* `FloatParser`
* `DoubleParser`
* `StringParser`
* `BlobParser`

此外，还可以从类构造函数创建行解析器。假设你有一个类：

```kotlin
data class Person(val firstName: String, val lastName: String, val age: Int)
```

解析器将非常简单：

```kotlin
val rowParser = classParser<Person>()
```

目前，如果主构造函数有可选参数，Anko**不支持**创建此类解析器。另外，请注意，构造函数将使用Java反射进行调用，因此对于大型数据集，编写自定义的`RowParser`更为合理。

如果您使用的是Anko`db.select()`建造者(builder)，您可以直接在其上调用`parseSingle`、`parseOpt`或`parseList`，并传递适当的解析器。

## 自定义行解析器

例如，让我们为列`（Int，String，String）`创建一个新的解析器。最简单的方法是：

```kotlin
class MyRowParser : RowParser<Triple<Int, String, String>> {
    override fun parseRow(columns: Array<Any>): Triple<Int, String, String> {
        return Triple(columns[0] as Int, columns[1] as String, columns[2] as String)
    }
}
```

现在我们的代码中有三个显式类型转换。让我们使用`rowParser`函数来摆脱它们：

```kotlin
val parser = rowParser { id: Int, name: String, email: String ->
    Triple(id, name, email)
}
```

就这样`rowParser`进行所有转换，您可以根据需要命名lambda参数。

## 游标流


Anko提供了一种以功能性方式访问SQLite`Cursor`的方法. 只需要 `cursor.asSequence()` 或 `cursor.asMapSequence()`扩展函数来获取行的顺序. 不要忘了关闭 `Cursor` :)

## 更新数据

让我们给一个用户起一个新名字:

```kotlin
update("User", "name" to "Alice")
    .where("_id = {userId}", "userId" to 42)
    .exec()
```

更新语句还有一个`whereSimple()`方法，为了用传统方式进行查询:

```kotlin
update("User", "name" to "Alice")
    .`whereSimple`("_id = ?", 42)
    .exec()
```

## 删除数据

让我们删除一行数据（注意delete方法没有任何`whereSimple()`方法；而是直接在参数中提供查询）:

```kotlin
val numRowsDeleted = delete("User", "_id = {userID}", "userID" to 37)
```

## 事务

有一个名为`transaction()`的特殊函数，它允许您在一个SQLite事务中进行多个数据库的操作。

```kotlin
transaction {
    // Your transaction code
}
```

如果在`{}`块内没有触发异常，则事务将被标记为成功。

<table>
<tr><td width="50px" align="center"></td>
<td>
<i>如果出于某种原因想要中止事务，只需抛出<code>TransactionAbortException</code>。在这种情况下，你不需要自己处理这个异常.</i>
</td>
</tr>
</table>
