package ir.ahvaz.mehra.local

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.DashboardRepository
import ir.ahvaz.mehra.domain.repository.FinanceRepository
import ir.ahvaz.mehra.domain.repository.GuestRepository
import ir.ahvaz.mehra.domain.repository.HousekeepingRepository
import ir.ahvaz.mehra.domain.repository.MaintenanceRepository
import ir.ahvaz.mehra.domain.repository.NotificationRepository
import ir.ahvaz.mehra.domain.repository.ReceptionRepository
import ir.ahvaz.mehra.domain.repository.ReportRepository
import ir.ahvaz.mehra.domain.repository.ReservationRepository
import ir.ahvaz.mehra.domain.repository.RoomRepository
import ir.ahvaz.mehra.domain.repository.UserRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the app (single build since 1.4.0).
 *
 * The app is fully local: these in-memory implementations (package `ir.ahvaz.mehra.local`)
 * back every repository. The network-backed implementations under `data/repository` still
 * compile but are no longer bound.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: DemoAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DemoDashboardRepository): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(impl: DemoRoomRepository): RoomRepository

    @Binds
    @Singleton
    abstract fun bindGuestRepository(impl: DemoGuestRepository): GuestRepository

    @Binds
    @Singleton
    abstract fun bindReservationRepository(impl: DemoReservationRepository): ReservationRepository

    @Binds
    @Singleton
    abstract fun bindReceptionRepository(impl: DemoReceptionRepository): ReceptionRepository

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: DemoFinanceRepository): FinanceRepository

    @Binds
    @Singleton
    abstract fun bindHousekeepingRepository(impl: DemoHousekeepingRepository): HousekeepingRepository

    @Binds
    @Singleton
    abstract fun bindMaintenanceRepository(impl: DemoMaintenanceRepository): MaintenanceRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: DemoUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: DemoReportRepository): ReportRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: DemoNotificationRepository): NotificationRepository
}
