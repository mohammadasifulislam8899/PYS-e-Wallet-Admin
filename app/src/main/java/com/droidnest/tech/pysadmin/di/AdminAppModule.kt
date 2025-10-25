// admin_app/di/AdminAppModule.kt
package com.droidnest.tech.pysadmin.di

import com.droidnest.tech.pysadmin.data.remote.FirebaseAdminAuthDataSource
import com.droidnest.tech.pysadmin.data.repository.AdminAuthRepositoryImpl
import com.droidnest.tech.pysadmin.data.repository.DashboardRepositoryImpl
import com.droidnest.tech.pysadmin.data.repository.ExchangeRateRepositoryImpl
import com.droidnest.tech.pysadmin.data.repository.KycManagementRepositoryImpl
import com.droidnest.tech.pysadmin.data.repository.AddMoneyPaymentMethodRepositoryImpl
import com.droidnest.tech.pysadmin.data.repository.PaymentMethodRepositoryImpl
import com.droidnest.tech.pysadmin.data.repository.TransactionRepositoryImpl
import com.droidnest.tech.pysadmin.data.repository.UserManagementRepositoryImpl
import com.droidnest.tech.pysadmin.domain.repository.AdminAuthRepository
import com.droidnest.tech.pysadmin.domain.repository.DashboardRepository
import com.droidnest.tech.pysadmin.domain.repository.ExchangeRateRepository
import com.droidnest.tech.pysadmin.domain.repository.KycManagementRepository
import com.droidnest.tech.pysadmin.domain.repository.AddMoneyPaymentMethodRepository
import com.droidnest.tech.pysadmin.domain.repository.PaymentMethodRepository
import com.droidnest.tech.pysadmin.domain.repository.TransactionRepository
import com.droidnest.tech.pysadmin.domain.repository.UserManagementRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdminAppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    // ============ Data Sources ============

    @Provides
    @Singleton
    fun provideFirebaseAdminAuthDataSource(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): FirebaseAdminAuthDataSource {
        return FirebaseAdminAuthDataSource(auth, firestore)
    }

    // ============ Repositories ============

    @Provides
    @Singleton
    fun provideAdminAuthRepository(
        dataSource: FirebaseAdminAuthDataSource
    ): AdminAuthRepository {
        return AdminAuthRepositoryImpl(dataSource)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        firestore: FirebaseFirestore
    ): TransactionRepository {
        return TransactionRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideKycManagementRepository(
        firestore: FirebaseFirestore
    ): KycManagementRepository {
        return KycManagementRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideUserManagementRepository(
        firestore: FirebaseFirestore
    ): UserManagementRepository {
        return UserManagementRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun providesDashboardRepository(
        fireStore: FirebaseFirestore
    ): DashboardRepository {
        return DashboardRepositoryImpl(fireStore)
    }

    @Provides
    @Singleton
    fun providesExchangeRateRepository(
        fireStore: FirebaseFirestore
    ): ExchangeRateRepository {
        return ExchangeRateRepositoryImpl(fireStore)
    }
    @Provides
    @Singleton
    fun providePaymentMethodRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): AddMoneyPaymentMethodRepository {
        return AddMoneyPaymentMethodRepositoryImpl(firestore, auth)
    }
    @Provides
    @Singleton
    fun provideWithdrawPaymentMethodRepository(
        firestore: FirebaseFirestore
    ): PaymentMethodRepository {
        return PaymentMethodRepositoryImpl(firestore)
    }
}