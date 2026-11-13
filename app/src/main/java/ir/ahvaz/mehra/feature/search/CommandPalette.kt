package ir.ahvaz.mehra.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.dataOrNull
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliFaOrDash
import ir.ahvaz.mehra.core.ui.components.Icon3D
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraSearchField
import ir.ahvaz.mehra.core.ui.components.rememberVoiceSearch
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.repository.ReservationFilter
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomFilterState
import ir.ahvaz.mehra.domain.repository.GuestRepository
import ir.ahvaz.mehra.domain.repository.ReservationRepository
import ir.ahvaz.mehra.domain.repository.RoomRepository
import ir.ahvaz.mehra.navigation.Routes
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One searchable record of the command palette. */
sealed interface SearchHit {
    val route: String
    val kindLabelFa: String

    data class RoomHit(val room: Room) : SearchHit {
        override val route: String get() = Routes.roomDetail(room.id)
        override val kindLabelFa: String get() = "اتاق"
    }

    data class GuestHit(val guest: Guest) : SearchHit {
        override val route: String get() = Routes.guestDetail(guest.id)
        override val kindLabelFa: String get() = "مهمان"
    }

    data class ReservationHit(val reservation: Reservation) : SearchHit {
        override val route: String get() = Routes.reservationDetail(reservation.id)
        override val kindLabelFa: String get() = "رزرو"
    }
}

data class PaletteUiState(
    val query: String = "",
    val results: List<SearchHit> = emptyList(),
    val searching: Boolean = false,
)

@HiltViewModel
class CommandPaletteViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val guestRepository: GuestRepository,
    private val reservationRepository: ReservationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PaletteUiState())
    val state: StateFlow<PaletteUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChange(raw: String) {
        _state.update { it.copy(query = raw, searching = raw.isNotBlank()) }
        searchJob?.cancel()
        if (raw.isBlank()) {
            _state.update { it.copy(results = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(250) // debounce while typing / after voice input
            val nq = normalizeSearchText(raw)
            val rooms = roomRepository.rooms(RoomFilterState(query = raw)).dataOrNull()?.data.orEmpty()
            val guests = guestRepository.quickSearch(raw).dataOrNull().orEmpty()
            val reservations = reservationRepository.list(ReservationFilter(query = raw)).dataOrNull()?.data.orEmpty()
            val results = buildList<SearchHit> {
                rooms.forEach { add(SearchHit.RoomHit(it)) }
                guests.forEach { add(SearchHit.GuestHit(it)) }
                reservations.forEach { add(SearchHit.ReservationHit(it)) }
            }.filter { hit -> matches(hit, nq) }
            _state.update { it.copy(results = results, searching = false) }
        }
    }

    private fun matches(hit: SearchHit, nq: String): Boolean = when (hit) {
        is SearchHit.RoomHit -> {
            val r = hit.room
            listOf(r.roomNumber, r.roomTypeName, r.status.labelFa, r.floor.toString(), r.currentGuestName.orEmpty())
                .any { normalizeSearchText(it).contains(nq) }
        }
        is SearchHit.GuestHit -> {
            val g = hit.guest
            listOf(g.fullName, g.phone.orEmpty(), g.nationalId.orEmpty(), g.carPlate.orEmpty(), g.city.orEmpty())
                .any { normalizeSearchText(it).contains(nq) }
        }
        is SearchHit.ReservationHit -> {
            val r = hit.reservation
            listOf(r.code, r.guestName, r.roomNumber.orEmpty(), r.status.labelFa)
                .any { normalizeSearchText(it).contains(nq) }
        }
    }

}

/**
 * Normalizes a search phrase for fuzzy Persian matching: lowercases, unifies Arabic/Persian
 * ی and ک, converts Persian digits to Latin and strips zero-width non-joiners.
 */
internal fun normalizeSearchText(raw: String): String = raw
    .lowercase()
    .replace('ي', 'ی')
    .replace('ك', 'ک')
    .replace('\u200c', ' ')
    .map { ch -> if (ch in '۰'..'۹') ('0' + (ch - '۰')) else ch }
    .joinToString("")
    .trim()

/**
 * Global command palette (v1.5.0): a Ctrl+K-style overlay that fuzzy-searches rooms, guests
 * and reservations at once and jumps straight to the record. The microphone button runs the
 * system speech recognizer (no app permission needed) and fills the query with what you said.
 */
@Composable
fun CommandPalette(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: CommandPaletteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var voiceError by remember { mutableStateOf<String?>(null) }

    val launchVoiceSearch = rememberVoiceSearch(
        prompt = "نام مهمان، شمارهٔ اتاق یا کد رزرو را بگویید",
        onResult = { spoken ->
            voiceError = null
            viewModel.onQueryChange(spoken)
        },
        onUnavailable = { voiceError = "تشخیص صدا در این دستگاه در دسترس نیست." },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "جست‌وجوی سراسری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = launchVoiceSearch) {
                        Icon(Icons.Rounded.Mic, contentDescription = "جست‌وجوی صوتی")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "بستن")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                MehraSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "اتاق، مهمان یا رزرو را جست‌وجو کنید…",
                )
                if (voiceError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    InlineMessage(text = voiceError!!, isError = true)
                }
                if (state.searching) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.query.isBlank()) {
                        item {
                            Text(
                                text = "نام یا شمارهٔ اتاق، مهمان یا رزرو را بنویسید — یا با دکمهٔ میکروفون بگویید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (!state.searching && state.results.isEmpty()) {
                        item {
                            Text(
                                text = "نتیجه‌ای یافت نشد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.results, key = { "${it.kindLabelFa}-${it.route}" }) { hit ->
                            SearchResultRow(hit = hit, onClick = { onNavigate(hit.route) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, onClick: () -> Unit) {
    MehraCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (hit) {
                is SearchHit.RoomHit -> Icon3D(
                    icon = Icons.Rounded.Bed,
                    contentDescription = null,
                    size = 38.dp,
                )
                is SearchHit.GuestHit -> Icon3D(
                    icon = Icons.Rounded.Person,
                    contentDescription = null,
                    size = 38.dp,
                )
                is SearchHit.ReservationHit -> Icon3D(
                    icon = Icons.Rounded.EventAvailable,
                    contentDescription = null,
                    size = 38.dp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.titleFa(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = hit.subtitleFa(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (hit) {
                is SearchHit.RoomHit -> StatusPill(text = hit.room.status.labelFa, tint = hit.room.status.tint())
                is SearchHit.ReservationHit -> StatusPill(text = hit.reservation.status.labelFa, tint = hit.reservation.status.tint())
                is SearchHit.GuestHit -> Text(
                    text = hit.kindLabelFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MehraTheme.colors.gold,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun SearchHit.titleFa(): String = when (this) {
    is SearchHit.RoomHit -> "اتاق ${room.roomNumber.toPersianDigits()}"
    is SearchHit.GuestHit -> guest.fullName.ifBlank { "بدون نام" }
    is SearchHit.ReservationHit -> "${reservation.code.toPersianDigits()} — ${reservation.guestName}"
}

private fun SearchHit.subtitleFa(): String = when (this) {
    is SearchHit.RoomHit -> listOfNotNull(
        room.roomTypeName.ifBlank { null },
        "طبقه ${room.floor.toString().toPersianDigits()}",
    ).joinToString(" • ")
    is SearchHit.GuestHit -> listOfNotNull(guest.phone, guest.city).joinToString(" • ").ifBlank { "—" }
    is SearchHit.ReservationHit -> listOfNotNull(
        "${reservation.checkIn.formatJalaliFaOrDash()} تا ${reservation.checkOut.formatJalaliFaOrDash()}",
        reservation.roomNumber?.let { "اتاق ${it.toPersianDigits()}" },
    ).joinToString(" • ")
}
