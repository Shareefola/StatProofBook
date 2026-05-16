package com.statproof.app.di

import android.content.Context
import androidx.room.Room
import com.statproof.data.db.StatProofDatabase
import com.statproof.data.db.dao.FavoriteDao
import com.statproof.data.db.dao.RecentHistoryDao
import com.statproof.data.db.dao.TheoremDao
import com.statproof.data.repository.PreferencesRepositoryImpl
import com.statproof.data.repository.ProofRepositoryImpl
import com.statproof.domain.repository.PreferencesRepository
import com.statproof.domain.repository.ProofRepository
import com.statproof.domain.usecase.CheckFavoriteUseCase
import com.statproof.domain.usecase.GenerateProofUseCase
import com.statproof.domain.usecase.GetAllTheoremsUseCase
import com.statproof.domain.usecase.GetAvailableTopicsUseCase
import com.statproof.domain.usecase.GetDatasetStatsUseCase
import com.statproof.domain.usecase.GetHomeDataUseCase
import com.statproof.domain.usecase.GetTheoremsByTopicUseCase
import com.statproof.domain.usecase.GetUserPreferencesUseCase
import com.statproof.domain.usecase.RecordProofViewUseCase
import com.statproof.domain.usecase.SearchTheoremsUseCase
import com.statproof.domain.usecase.ToggleFavoriteUseCase
import com.statproof.domain.usecase.UpdateUserPreferencesUseCase
import com.statproof.proofengine.engine.ProofEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ── Database Module ───────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StatProofDatabase =
        Room.databaseBuilder(
            context,
            StatProofDatabase::class.java,
            StatProofDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTheoremDao(db: StatProofDatabase): TheoremDao = db.theoremDao()

    @Provides
    fun provideFavoriteDao(db: StatProofDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideRecentHistoryDao(db: StatProofDatabase): RecentHistoryDao = db.recentHistoryDao()
}

// ── Repository Bindings ───────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProofRepository(impl: ProofRepositoryImpl): ProofRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}

// ── Proof Engine Module ───────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object ProofEngineModule {

    @Provides
    @Singleton
    fun provideProofEngine(): ProofEngine = ProofEngine()
}

// ── Use Case Module ───────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetAllTheoremsUseCase(repo: ProofRepository) =
        GetAllTheoremsUseCase(repo)

    @Provides
    fun provideGetTheoremsByTopicUseCase(repo: ProofRepository) =
        GetTheoremsByTopicUseCase(repo)

    @Provides
    fun provideGenerateProofUseCase(
        proofRepo: ProofRepository,
        prefRepo: PreferencesRepository,
    ) = GenerateProofUseCase(proofRepo, prefRepo)

    @Provides
    fun provideSearchTheoremsUseCase(repo: ProofRepository) =
        SearchTheoremsUseCase(repo)

    @Provides
    fun provideGetHomeDataUseCase(repo: ProofRepository) =
        GetHomeDataUseCase(repo)

    @Provides
    fun provideRecordProofViewUseCase(repo: ProofRepository) =
        RecordProofViewUseCase(repo)

    @Provides
    fun provideToggleFavoriteUseCase(repo: ProofRepository) =
        ToggleFavoriteUseCase(repo)

    @Provides
    fun provideCheckFavoriteUseCase(repo: ProofRepository) =
        CheckFavoriteUseCase(repo)

    @Provides
    fun provideGetAvailableTopicsUseCase(repo: ProofRepository) =
        GetAvailableTopicsUseCase(repo)

    @Provides
    fun provideGetUserPreferencesUseCase(repo: PreferencesRepository) =
        GetUserPreferencesUseCase(repo)

    @Provides
    fun provideUpdateUserPreferencesUseCase(repo: PreferencesRepository) =
        UpdateUserPreferencesUseCase(repo)

    @Provides
    fun provideGetDatasetStatsUseCase(repo: ProofRepository) =
        GetDatasetStatsUseCase(repo)
}
