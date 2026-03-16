package com.minebot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme {

                var status by remember { mutableStateOf("Idle") }
                val scope = rememberCoroutineScope()

                Surface {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "MineBot",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(status)

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    status = "Connecting..."

                                    try {
                                        val res = ApiClient.getHealth()
                                        status = "Server OK"
                                    } catch (e: Exception) {
                                        status = "Connection failed"
                                    }
                                }
                            }
                        ) {
                            Text("Test Backend")
                        }

                    }

                }

            }

        }
    }

}
