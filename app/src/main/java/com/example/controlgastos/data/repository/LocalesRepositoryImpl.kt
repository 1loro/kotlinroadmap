package com.example.controlgastos.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.controlgastos.data.local.IngresoDao
import com.example.controlgastos.data.local.IngresoEntity
import com.example.controlgastos.data.local.aModelo
import com.example.controlgastos.domain.model.Ingreso
import com.example.controlgastos.domain.model.Tema
import com.example.controlgastos.domain.model.TipoIngreso
import com.example.controlgastos.domain.repository.IngresosRepository
import com.example.controlgastos.domain.repository.PreferenciasRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

// Datos que existen solo en este teléfono.

class IngresosRepositoryImpl @Inject constructor(private val dao: IngresoDao) : IngresosRepository {

    override fun ingresosDelMes(mes: YearMonth): Flow<List<Ingreso>> =
        dao.observarEntre(mes.atDay(1).toEpochDay(), mes.atEndOfMonth().toEpochDay())
            .map { lista -> lista.map { it.aModelo() } }

    override suspend fun agregar(monto: Double, tipo: TipoIngreso, fecha: LocalDate, nota: String?) =
        dao.insertar(IngresoEntity(monto = monto, tipo = tipo, nota = nota, fecha = fecha))

    override suspend fun eliminar(id: Long) = dao.borrar(id)
}

/** Tema claro/oscuro/sistema. No es un dato sensible: preferencias normales. */
@Singleton
class PreferenciasRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : PreferenciasRepository {
    private val preferencias = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    private val _tema = MutableStateFlow(
        preferencias.getString(CLAVE_TEMA, null)?.let { runCatching { Tema.valueOf(it) }.getOrNull() } ?: Tema.SISTEMA,
    )
    override val tema: StateFlow<Tema> = _tema.asStateFlow()

    override fun elegirTema(tema: Tema) {
        preferencias.edit { putString(CLAVE_TEMA, tema.name) }
        _tema.value = tema
    }

    private companion object {
        const val CLAVE_TEMA = "tema"
    }
}
