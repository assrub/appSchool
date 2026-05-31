package com.appenglish.di

import com.appenglish.data.local.AppDatabase
import com.appenglish.data.remote.api.ContentApi
import com.appenglish.data.remote.api.TtsApi
import com.appenglish.data.remote.api.TranslateApi
import com.appenglish.data.remote.api.ProgressApi
import com.appenglish.data.remote.api.DictionaryApi
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.data.repository.DictionaryRepository
import com.appenglish.util.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideContentApi(retrofit: Retrofit): ContentApi =
        retrofit.create(ContentApi::class.java)

    @Provides
    @Singleton
    fun provideTtsApi(retrofit: Retrofit): TtsApi =
        retrofit.create(TtsApi::class.java)

    @Provides
    @Singleton
    fun provideTranslateApi(retrofit: Retrofit): TranslateApi =
        retrofit.create(TranslateApi::class.java)

    @Provides
    @Singleton
    fun provideProgressApi(retrofit: Retrofit): ProgressApi =
        retrofit.create(ProgressApi::class.java)

    @Provides
    @Singleton
    fun provideDictionaryApi(retrofit: Retrofit): DictionaryApi =
        retrofit.create(DictionaryApi::class.java)

    @Provides
    @Singleton
    fun provideAppDatabase(application: android.app.Application): AppDatabase =
        AppDatabase.getInstance(application)

    @Provides
    @Singleton
    fun provideContentRepository(api: ContentApi): ContentRepository =
        ContentRepository(api)

    @Provides
    @Singleton
    fun provideProgressRepository(
        api: ProgressApi,
        db: AppDatabase
    ): ProgressRepository = ProgressRepository(api, db.progressDao())

    @Provides
    @Singleton
    fun provideDictionaryRepository(
        api: DictionaryApi,
        db: AppDatabase
    ): DictionaryRepository = DictionaryRepository(api, db.dictionaryDao())
}
