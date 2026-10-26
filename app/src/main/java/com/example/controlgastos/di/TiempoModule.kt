package com.example.controlgastos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * El "reloj" también es una dependencia. Si el código llamara directamente
 * a LocalDate.now(), un test no podría saber qué fecha esperar; inyectando un
 * Clock, el test usa uno fijo (Clock.fixed) y el resultado es predecible.
 */
@Module
@InstallIn(SingletonComponent::class)
object TiempoModule {
    @Provides
    fun reloj(): Clock = Clock.systemDefaultZone()
}
