package ir.ahvaz.mehra.core.data

import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.ErrorKind
import ir.ahvaz.mehra.core.common.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager

/**
 * Personal-test defaults for the direct SQL Server connection (LAN/WiFi, port 1433).
 *
 * WARNING: anything compiled into the APK is extractable — these credentials are for the
 * owner's own test server only and must never ship in a production build.
 */
object DirectDbDefaults {
    const val PORT = "1433"
    const val USER = "adminan"
    const val PASSWORD = "St@R2022$"
    const val DATABASE = "Mehra"
}

data class DbTable(val schemaName: String, val name: String) {
    val fullName: String get() = "$schemaName.$name"
}

data class DbGrid(val columns: List<String>, val rows: List<List<String?>>)

/**
 * Minimal direct-TDS client (SQL Server over port 1433) used ONLY by the personal test
 * screen (feature/dbtool): list databases, list tables, run a read-only query.
 * The rest of the app never uses it — it works on the local in-memory store.
 */
object DirectDb {

    init {
        runCatching { Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver") }
    }

    private fun connStr(host: String, port: String, db: String, user: String, pass: String): String =
        "jdbc:sqlserver://$host:$port;databaseName=$db;user=$user;password=$pass;" +
            "encrypt=true;trustServerCertificate=true;loginTimeout=8"

    private fun mapError(ex: Exception, host: String, port: String): String = when {
        ex.message?.contains("Login failed", ignoreCase = true) == true ->
            "ورود به SQL Server ناموفق بود؛ نام کاربری/رمز یا فعال بودن SQL Server Authentication را بررسی کنید. (${ex.message})"
        ex.message?.contains("TCP Provider", ignoreCase = true) == true ||
            ex.message?.contains("Connection refused", ignoreCase = true) == true ->
            "اتصال TCP به $host:$port برقرار نشد؛ IP سرور روی WiFi، پورت ۱۴۳۳ و Rule فایروال را چک کنید. (${ex.message})"
        ex.message?.contains("Cannot open database", ignoreCase = true) == true ->
            "پایگاه داده باز نشد (کد 4060): نام پایگاه داده را چک کنید. (${ex.message})"
        else -> "خطای پایگاه داده: ${ex.message}"
    }

    suspend fun listDatabases(host: String, port: String, user: String, pass: String): AppResult<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                DriverManager.getConnection(connStr(host, port, "master", user, pass)).use { con ->
                    con.createStatement().use { st ->
                        st.executeQuery(
                            "SELECT name FROM sys.databases WHERE database_id > 4 ORDER BY name"
                        ).use { rs ->
                            val out = mutableListOf<String>()
                            while (rs.next()) out.add(rs.getString(1))
                            AppResult.Success(out)
                        }
                    }
                }
            } catch (ex: Exception) {
                AppResult.Failure(AppError(ErrorKind.NETWORK, message = mapError(ex, host, port), cause = ex))
            }
        }

    suspend fun listTables(host: String, port: String, db: String, user: String, pass: String): AppResult<List<DbTable>> =
        withContext(Dispatchers.IO) {
            try {
                DriverManager.getConnection(connStr(host, port, db, user, pass)).use { con ->
                    con.createStatement().use { st ->
                        st.executeQuery(
                            "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_SCHEMA, TABLE_NAME"
                        ).use { rs ->
                            val out = mutableListOf<DbTable>()
                            while (rs.next()) out.add(DbTable(rs.getString(1), rs.getString(2)))
                            AppResult.Success(out)
                        }
                    }
                }
            } catch (ex: Exception) {
                AppResult.Failure(AppError(ErrorKind.NETWORK, message = mapError(ex, host, port), cause = ex))
            }
        }

    /** Runs a read-only query and returns at most [maxRows] rows. */
    suspend fun query(
        host: String, port: String, db: String, user: String, pass: String,
        sql: String, maxRows: Int = 200,
    ): AppResult<DbGrid> = withContext(Dispatchers.IO) {
        try {
            DriverManager.getConnection(connStr(host, port, db, user, pass)).use { con ->
                con.createStatement().use { st ->
                    st.maxRows = maxRows
                    st.executeQuery(sql).use { rs ->
                        val meta = rs.metaData
                        val cols = (1..meta.columnCount).map { meta.getColumnLabel(it) }
                        val rows = mutableListOf<List<String?>>()
                        while (rs.next()) {
                            rows.add((1..meta.columnCount).map { i -> rs.getString(i) })
                        }
                        AppResult.Success(DbGrid(cols, rows))
                    }
                }
            }
        } catch (ex: Exception) {
            AppResult.Failure(AppError(ErrorKind.NETWORK, message = mapError(ex, host, port), cause = ex))
        }
    }
}
