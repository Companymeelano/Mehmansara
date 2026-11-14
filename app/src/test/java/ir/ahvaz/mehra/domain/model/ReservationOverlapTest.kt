package ir.ahvaz.mehra.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class ReservationOverlapTest {

    private fun reservation(
        status: ReservationStatus,
        checkIn: LocalDate = LocalDate.of(2026, 11, 10),
        checkOut: LocalDate = LocalDate.of(2026, 11, 12),
    ) = Reservation(
        id = "r1",
        code = "R-1",
        guestId = "g1",
        guestName = "مهمان تست",
        checkIn = checkIn,
        checkOut = checkOut,
        status = status,
    )

    @Test
    fun `confirmed reservation covers its nights but not checkout day`() {
        val r = reservation(ReservationStatus.CONFIRMED)
        assertThat(r.isActiveOn(LocalDate.of(2026, 11, 9))).isFalse()
        assertThat(r.isActiveOn(LocalDate.of(2026, 11, 10))).isTrue()
        assertThat(r.isActiveOn(LocalDate.of(2026, 11, 11))).isTrue()
        assertThat(r.isActiveOn(LocalDate.of(2026, 11, 12))).isFalse()
        assertThat(r.isActiveOn(LocalDate.of(2026, 11, 13))).isFalse()
    }

    @Test
    fun `finished or cancelled reservations are inactive`() {
        val day = LocalDate.of(2026, 11, 11)
        assertThat(reservation(ReservationStatus.CHECKED_OUT).isActiveOn(day)).isFalse()
        assertThat(reservation(ReservationStatus.CANCELLED).isActiveOn(day)).isFalse()
        assertThat(reservation(ReservationStatus.EXPIRED).isActiveOn(day)).isFalse()
        assertThat(reservation(ReservationStatus.NO_SHOW).isActiveOn(day)).isFalse()
        assertThat(reservation(ReservationStatus.CHECKED_IN).isActiveOn(day)).isTrue()
        assertThat(reservation(ReservationStatus.PENDING).isActiveOn(day)).isTrue()
        assertThat(reservation(ReservationStatus.WAITLIST).isActiveOn(day)).isTrue()
    }
}
