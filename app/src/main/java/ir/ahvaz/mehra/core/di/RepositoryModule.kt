package ir.ahvaz.mehra.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.ahvaz.mehra.data.repository.AuthRepositoryImpl
import ir.ahvaz.mehra.data.repository.DashboardRepositoryImpl
import ir.ahvaz.mehra.data.repository.FinanceRepositoryImpl
import ir.ahvaz.mehra.data.repository.GuestRepositoryImpl
import ir.ahvaz.mehra.data.repository.HousekeepingRepositoryImpl
import ir.ahvaz.mehra.data.repository.MaintenanceRepositoryImpl
import ir.ahvaz.mehra.data.repository.NotificationRepositoryImpl
import ir.ahvaz.mehra.data.repository.ReceptionRepositoryImpl
import ir.ahvaz.mehra.data.repository.ReportRepositoryImpl
import ir.ahvaz.mehra.data.repository.ReservationRepositoryImpl
import ir.ahvaz.mehra.data.repository.RoomRepositoryImpl
import ir.ahvaz.mehra.data.repository.UserRepositoryImpl
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

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(impl: RoomRepositoryImpl): RoomRepository

    @Binds
    @Singleton
    abstract fun bindGuestRepository(impl: GuestRepositoryImpl): GuestRepository

    @Binds
    @Singleton
    abstract fun bindReservationRepository(impl: ReservationRepositoryImpl): ReservationRepository

    @Binds
    @Singleton
    abstract fun bindReceptionRepository(impl: ReceptionRepositoryImpl): ReceptionRepository

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: FinanceRepositoryImpl): FinanceRepository

    @Binds
    @Singleton
    abstract fun bindHousekeepingRepository(impl: HousekeepingRepositoryImpl): HousekeepingRepository

    @Binds
    @Singleton
    abstract fun bindMaintenanceRepository(impl: MaintenanceRepositoryImpl): MaintenanceRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
