package ir.ahvaz.mehra.local

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import ir.ahvaz.mehra.core.security.SecureStore
import ir.ahvaz.mehra.domain.model.ActivityItem
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.AuditLogEntry
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.Stay
import ir.ahvaz.mehra.domain.model.Transaction
import java.time.Instant
import java.time.LocalDate

/**
 * JSON snapshot of the whole local store, persisted into the encrypted [SecureStore]
 * so data survives app restarts (since 1.4.3 the local store is durable on-device).
 */
data class StoreSnapshot(
    val rooms: List<Room> = emptyList(),
    val guests: List<Guest> = emptyList(),
    val reservations: List<Reservation> = emptyList(),
    val stays: List<Stay> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val housekeepingTasks: List<HousekeepingTask> = emptyList(),
    val maintenanceTickets: List<MaintenanceTicket> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val users: List<AppUser> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val sequence: Int = 4210,
)

object LocalStorePersistence {

    const val KEY = "local_store_json"

    private val instantAdapter = object : TypeAdapter<Instant>() {
        override fun write(out: JsonWriter, value: Instant?) {
            if (value == null) out.nullValue() else out.value(value.toEpochMilli())
        }
        override fun read(reader: JsonReader): Instant? =
            if (reader.peek() == com.google.gson.stream.JsonToken.NULL) { reader.nextNull(); null }
            else Instant.ofEpochMilli(reader.nextLong())
    }

    private val localDateAdapter = object : TypeAdapter<LocalDate>() {
        override fun write(out: JsonWriter, value: LocalDate?) {
            if (value == null) out.nullValue() else out.value(value.toString())
        }
        override fun read(reader: JsonReader): LocalDate? =
            if (reader.peek() == com.google.gson.stream.JsonToken.NULL) { reader.nextNull(); null }
            else LocalDate.parse(reader.nextString())
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, instantAdapter)
        .registerTypeAdapter(LocalDate::class.java, localDateAdapter)
        .create()

    fun load(store: SecureStore): StoreSnapshot? = runCatching {
        val json = store.getString(KEY) ?: return null
        gson.fromJson(json, StoreSnapshot::class.java)
    }.getOrNull()

    fun save(store: SecureStore, snapshot: StoreSnapshot) = runCatching {
        store.putString(KEY, gson.toJson(snapshot))
    }
}

/**
 * A MutableList that fires [onChange] after every mutation, so the store can re-persist
 * itself automatically no matter which repository edited which list.
 */
class PersistingList<T>(
    initial: List<T> = emptyList(),
    private val onChange: () -> Unit,
) : MutableList<T> {

    private val delegate = initial.toMutableList<T>()

    override val size: Int get() = delegate.size
    override fun contains(element: T): Boolean = delegate.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = delegate.containsAll(elements)
    override fun get(index: Int): T = delegate[index]
    override fun indexOf(element: T): Int = delegate.indexOf(element)
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun iterator(): MutableIterator<T> = delegate.iterator()
    override fun lastIndexOf(element: T): Int = delegate.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<T> = delegate.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = delegate.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = delegate.subList(fromIndex, toIndex)

    override fun add(element: T): Boolean { val r = delegate.add(element); onChange(); return r }
    override fun add(index: Int, element: T) { delegate.add(index, element); onChange() }
    override fun addAll(elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false
        val r = delegate.addAll(elements); onChange(); return r
    }
    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false
        val r = delegate.addAll(index, elements); onChange(); return r
    }
    override fun clear() { if (delegate.isEmpty()) return; delegate.clear(); onChange() }
    override fun remove(element: T): Boolean { val r = delegate.remove(element); if (r) onChange(); return r }
    override fun removeAll(elements: Collection<T>): Boolean { val r = delegate.removeAll(elements); if (r) onChange(); return r }
    override fun removeAt(index: Int): T { val v = delegate.removeAt(index); onChange(); return v }
    override fun retainAll(elements: Collection<T>): Boolean { val r = delegate.retainAll(elements); if (r) onChange(); return r }
    override fun set(index: Int, element: T): T { val v = delegate.set(index, element); onChange(); return v }
}
