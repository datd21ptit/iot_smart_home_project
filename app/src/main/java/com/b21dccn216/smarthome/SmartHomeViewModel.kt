package com.b21dccn216.smarthome


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b21dccn216.smarthome.model.Destinations.DASHBOARD
import com.b21dccn216.smarthome.model.Destinations.SENSOR_DATA_TABLE
import com.b21dccn216.smarthome.data.SmartHomeRepository
import com.b21dccn216.smarthome.model.AppState.LOADED
import com.b21dccn216.smarthome.model.AppState.LOADING
import com.b21dccn216.smarthome.model.DashboarUiState
import com.b21dccn216.smarthome.model.TableResponse
import com.b21dccn216.smarthome.model.TableUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SmartHomeViewmodel(
    private val repository: SmartHomeRepository
) : ViewModel(){

    private val _uiStateDashboard = MutableStateFlow(DashboarUiState())
    val uiStateDashboard: StateFlow<DashboarUiState> = _uiStateDashboard.asStateFlow()

    private val _appState = MutableStateFlow(LOADING)
    val appState = _appState.asStateFlow()
    private val _uiStateTable = MutableStateFlow(
        TableUiState(
            tableData = TableResponse(
                data = listOf(listOf("id", "row1", "row2", "row3", "time")),
                page = 1,
                totalRows = 100,
                totalPages = 10
                )
        )
    )
    val uiStateTable: StateFlow<TableUiState> = _uiStateTable.asStateFlow()

    private val _currentScreen = MutableStateFlow(DASHBOARD)

    init {
        viewModelScope.launch {
            initDeviceState()
//            val result = repository.getSensorDataTable(_uiStateTable.value)
//            _uiStateTable.update { value ->
//                value.copy(
//                    tableData = result
//                )
//            }
        }.invokeOnCompletion {
            _appState.value = LOADED
        }

        viewModelScope.launch {
            _currentScreen.collectLatest{ current ->
                when(current){
                    DASHBOARD -> getDashboardData()
                    SENSOR_DATA_TABLE -> getTableData()
                }
            }
        }
    }

    private fun getTableData(){
        viewModelScope.launch {
            while (_currentScreen.value == SENSOR_DATA_TABLE){
                try{
                    val result = repository.getSensorDataTable(_uiStateTable.value)
                    _uiStateTable.update { value ->
                        value.copy(
                            tableData = result
                        )
                    }
                    _appState.value = LOADED
                    Log.d("viewmodel", "get table data")
                }catch (e: Exception){
                    Log.e("viewmodel", e.toString())
                }
                delay(2000)
            }
        }
    }
    private fun getDashboardData(){
        viewModelScope.launch {
            while(_currentScreen.value == DASHBOARD){
                try {
                    val listResult = repository.getSensorData()
                    _uiStateDashboard.update { value ->
                        value.copy(
                            temp = listResult[0].temp,
                            humid = listResult[0].humid,
                            light = listResult[0].light,
                        ).also {
                            value.listTemp.add(listResult[0].temp)
                            value.listHumid.add(listResult[0].humid)
                            value.listLight.add(listResult[0].light)
                            value.listHumid.removeAt(0)
                            value.listTemp.removeAt(0)
                            value.listHumid.removeAt(0)
                        }
                    }
                    Log.d("viewmodel", uiStateDashboard.value.listTemp.size.toString())
                }catch (e: Exception){
                    Log.e("viewmodel", e.toString())
                }
                delay(2000)
            }
        }
    }
    private suspend fun initDeviceState(){
        try {
            val ret: DashboarUiState = repository.getSensorData()[0]
            _uiStateDashboard.update { newValue ->
                newValue.copy(
                    led = ret.led,
                    fan = ret.fan,
                    relay = ret.relay
                )
            }
            val listTemp = repository.getChartData("temp").toMutableList()
            listTemp.removeIf{ it == 0}
            _uiStateDashboard.update { newValue ->
                newValue.copy(listTemp = listTemp)
            }
            val listHumid = repository.getChartData("humid").toMutableList()
            listHumid.removeIf{ it == 0}
            _uiStateDashboard.update { newValue ->
                newValue.copy(listHumid = listHumid)
            }
            val listLight = repository.getChartData("light").toMutableList()
            listLight.removeIf{ it == 0}
            _uiStateDashboard.update { newValue ->
                newValue.copy(listLight = listLight)
            }

            Log.e("viewmodel", "init device state function")
        }catch (e: Exception){
            Log.e("viewmodel", e.toString())
        }

    }


    fun clickAction(state: DashboarUiState){
        viewModelScope.launch {
            try {
                val response = repository.senAction(led = state.led, fan = state.fan, relay = state.relay)
                Log.d("viewmodel", response.toString())
                if(response.isSuccessful && response.code() == 200){
                    _uiStateDashboard.update { value ->
                        value.copy(
                            led = state.led,
                            fan = state.fan,
                            relay = state.relay
                        )
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun navigateTo(screen: String){
        _currentScreen.value = screen
        if(screen != DASHBOARD){
            _appState.value = LOADING
        }

    }

    fun moveToPage(state: TableUiState){
        _uiStateTable.update { value ->
            value.copy(
                page = state.page,
                row1 = state.row1,
                row2 = state.row2,
                row3 = state.row3,
                time = state.time
            )
        }
    }

}


