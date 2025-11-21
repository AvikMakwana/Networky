# 🌐 NetState  
### Lightweight, Modern & Reactive Android Network Monitoring Library

<div align="center">

![Maven Central](https://img.shields.io/maven-central/v/com.avikmakwana/netstate?color=brightgreen&style=for-the-badge)
![Android](https://img.shields.io/badge/Android-Network%20Monitoring-green?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue?logo=kotlin&style=for-the-badge)
![License](https://img.shields.io/badge/License-Apache%202.0-yellow?style=for-the-badge)

</div>

---

## ✨ What is NetState?

**NetState** is a blazing-fast, tiny, lifecycle-aware Android library that helps developers detect:

- 🌐 **Real-time Internet On/Off**
- 📶 **Network Type** (WiFi / Mobile / None)
- ⚡ **Instant Connectivity State**
- 🌀 **Reactive Flows for network events**
- 🔥 Zero-boilerplate, tiny footprint, production ready

Designed with modern Android development practices using **Kotlin**, **Coroutines**, **Flow**, and **Clean Architecture**.

---

# 📦 Installation

Add this to your **module-level** `build.gradle`:

```kotlin
dependencies {
    implementation("com.avikmakwana:netstate:1.0.0")
}
```

That's it. No setup. No permissions needed. Plug & play. 🚀

---

# 🧩 Usage

## 🔌 Initialize

```kotlin
private val netState by lazy { NetStateMonitor(applicationContext) }
```

---

## 📡 Observe Network State (Flow-based)

```kotlin
lifecycleScope.launch {
    netState.networkState.collect { state ->
        when (state) {
            is NetworkState.Connected -> {
                Log.d("NetState", "Connected: ${state.type}")
            }
            NetworkState.Disconnected -> {
                Log.d("NetState", "Disconnected")
            }
        }
    }
}
```

---

## 🌐 Check Current Connectivity

```kotlin
val isOnline = netState.isConnected()
val type = netState.currentNetworkType()
```

---

## 🧭 Network Types

```kotlin
NetworkType.WIFI
NetworkType.MOBILE
NetworkType.NONE
```

---

## 🧪 Jetpack Compose Example

```kotlin
@Composable
fun NetworkStatusText(netState: NetStateMonitor) {
    val state by netState.networkState.collectAsState(initial = NetworkState.Disconnected)

    Text(
        text = when (state) {
            is NetworkState.Connected -> "Online (${(state as NetworkState.Connected).type})"
            NetworkState.Disconnected -> "Offline"
        }
    )
}
```

---

# 👨‍💻 Author

**Avinash Makwana**  
Android Developer (3+ YOE) | Health-Tech | AI-Driven App Enthusiast  

🌟 Passionate about modern Android development using Kotlin, Coroutines, Flow, Clean Architecture, and Jetpack Compose.  
🔊 Built real-time translation & speech systems at WeHear, integrating cloud services (GCP Translation, Speech Recognition) and offline models.  
🤖 Exploring Android × AI — currently conceptualizing **Pill Pocket**, an AI-powered medication assistant.

---

# 🔗 Connect With Me

| Platform | Link |
|---------|------|
| 🌐 GitHub | https://github.com/AvikMakwana |
| 💼 LinkedIn | https://www.linkedin.com/in/avikmakwana/ |

---

# 📜 License

```yaml
Apache License 2.0
Copyright 2025
```

<div align="center">

⭐ **If you like this library, please give it a star on GitHub!**  
Your support motivates further development.

</div>
