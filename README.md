# 📻 Flippy - App Nativa Android de Mensajería de Voz ("Walkie-Talkie")

**Flippy** es una aplicación 100% nativa para Android desarrollada en **Kotlin** y **Jetpack Compose**, diseñada para comunicación de voz instantánea Push-to-Talk (Walkie-Talkie) de ultra baja latencia sin requerir número de teléfono personal.

El proyecto está estructurado con **Gradle Kotlin DSL (`build.gradle.kts`)** para abrirse directamente desde la raíz en **Android Studio**.

---

## 📱 Características y Arquitectura Nativa

### 1. Autenticación y Perfil sin Número Telefónico
- **ID Único Táctico:** Identificador generado por la app (ej. `FLIP-8924A`) con alias y avatar.
- **Códigos QR de Alta Definición:** Generador nativo de código QR con **ZXing** para compartir y emparejar contactos al instante.
- **Escáner QR en Tiempo Real:** Visor nativo con **CameraX** y análisis de códigos de barras.
- **Gestión de Contactos:** Lista de amigos con indicador en vivo y botón directo de llamada PTT 1 a 1.

### 2. Motor de Audio Nativo de Ultra Baja Latencia
- **Grabación PCM Nativa (`AudioRecord`):** Captura de audio en tiempo real a 44.1 kHz, 16-bit Mono.
- **Medidor VU LED en Vivo:** Cálculo instantáneo de valor RMS del micrófono y visualización en barra de 16 segmentos LED reactivos.
- **Osciloscopio en Canvas Compose:** Renderizado dinámico de la onda de audio y modulación en pantalla táctica LCD.
- **Efectos de Sonido Tácticos:**
  - *Roger Beep Motorola* dual-tone sintetizado en PCM (1150Hz -> 880Hz + cola de estática squelch).
  - *Click Mecánico PTT* al pulsar y soltar.
  - *Click Analógico de Dial* al sintonizar frecuencias.
  - *Chirp Táctico* de audio entrante.

### 3. Filtros de Voz DSP en Tiempo Real
- 📻 **Radio Walkie:** Filtro pasa-banda 350Hz–3.2kHz con saturación analógica y estática.
- 🎙️ **Voz Limpia:** Audio PCM sin efectos.
- 🤖 **Cyborg Robot:** Modulación en anillo a 52Hz.
- 🛡️ **Voz Grave / Espía:** Realce de subgraves y pitch down.
- ✨ **Helio / Agudo:** Formantes agudos.
- 📢 **Megáfono:** Altavoz táctico con eco slapback de 40ms.
- 🛸 **Alien Espacial:** Oscilador FM extraterrestre.

### 4. Servicio en Segundo Plano (Background & Lock Screen)
- **`WalkieTalkieForegroundService`:** Servicio en primer plano nativo de Android con notificación persistente (`microphone`), permitiendo recibir y reproducir audio incluso con el teléfono bloqueado o con la app minimizada.

### 5. Comunicación en Tiempo Real
- **WebSocket Nativo con OkHttp:** Protocolo bidireccional de baja latencia para señalización PTT (`ptt_start`, `ptt_end`), medición de Ping/RTT en vivo y entrega instantánea de paquetes de voz.

---

## 📂 Estructura del Proyecto

```
Flippy/
├── build.gradle.kts               # Configuración Gradle raíz (Kotlin DSL)
├── settings.gradle.kts            # Módulos del proyecto (:app)
├── gradle.properties              # Configuración de memoria JVM
├── gradlew & gradlew.bat          # Wrapper de Gradle
├── gradle/wrapper/                # Wrapper Gradle 8.7
├── app/
│   ├── build.gradle.kts           # Dependencias Jetpack Compose, CameraX, OkHttp, ZXing
│   └── src/main/
│       ├── AndroidManifest.xml    # Permisos (Audio, Cámara, ForegroundService, Vibración)
│       ├── kotlin/com/flippy/walkietalkie/
│       │   ├── FlippyApp.kt       # Application class & Notification Channels
│       │   ├── MainActivity.kt    # Entrypoint Compose & Navegación
│       │   ├── audio/             # AudioRecord, AudioPlayer, SoundEffects, VoiceFilterProcessor
│       │   ├── data/model/        # User, Channel, VoiceMessage, VoiceFilter
│       │   ├── network/           # WebSocketClient OkHttp en tiempo real
│       │   ├── service/           # WalkieTalkieForegroundService
│       │   └── ui/                # Theme, Components (PttButton, VuMeter, Oscilloscope), Screens
│       └── res/                   # Drawables, Colores, Strings y Temas
└── backend/                       # Servidor WebSocket & Audio en Node.js
    ├── package.json
    └── server.js
```

---

## 🚀 ¿Cómo Abrir y Ejecutar en Android Studio?

1. Abre **Android Studio**.
2. Selecciona **"Open"** y elige directamente la carpeta raíz del proyecto:
   ```
   /home/user/Flippy
   ```
3. Espera a que Android Studio realice el **Gradle Sync** automático.
4. Conecta tu teléfono Android físico con depuración USB habilitada o inicia un Emulador (AVD con Android 8.0+).
5. Pulsa el botón verde **Run ▶️** (o presiona `Shift + F10`).

---

## 📡 Ejecutar el Servidor Backend WebSocket

El backend en tiempo real se encuentra en la carpeta `backend/`:

```bash
cd backend
npm install
npm start
```

El servidor quedará activo en el puerto `3000` (`ws://localhost:3000/ws`). En el emulador de Android se conecta automáticamente a `ws://10.0.2.2:3000/ws`.
