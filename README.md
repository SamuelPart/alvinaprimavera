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

### 5. Comunicación en Tiempo Real (Supabase en la nube)
- **Supabase Realtime:** los mensajes de voz y la señalización PTT (`ptt start/end`) viajan como filas en tablas Postgres y llegan al instante por suscripción Realtime — sin servidor propio.
- **Supabase Storage:** los audios WAV se suben al bucket público `flippy-audio` y cualquier dispositivo los descarga y reproduce.
- **Supabase Postgrest:** perfiles, canales, contactos e historial de mensajes persisten en la nube.
- **Fallback offline:** si no configuras credenciales de Supabase, la app usa automáticamente el servidor WebSocket local con OkHttp (`backend/server.js`).

---

## ☁️ Conectar con Supabase (paso a paso)

Toda la app funciona sobre Supabase cuando hay credenciales configuradas. El código ya está listo (`SupabaseRepository.kt` + `FlippyViewModel.kt`); solo falta tu proyecto en la nube:

1. **Crea un proyecto** gratis en [supabase.com](https://supabase.com).
2. **Crea las tablas, el bucket y Realtime:** en tu proyecto abre **SQL Editor → New query**, pega el contenido **completo** de [`supabase/schema.sql`](supabase/schema.sql) y pulsa **Run**. Esto crea:
   - Tablas: `profiles`, `channels`, `contacts`, `voice_messages`, `ptt_events`
   - Bucket público de Storage: `flippy-audio`
   - Publicación Realtime para `voice_messages` y `ptt_events`

   > ⚠️ Si tu proyecto ya tenía tablas viejas de Flippy con otra estructura y el script da un error de tipo (ej. `invalid input syntax for type uuid`), ejecuta primero [`supabase/reset.sql`](supabase/reset.sql) **una sola vez** (borra las 5 tablas viejas) y luego vuelve a correr `schema.sql` completo.
3. **Copia tus credenciales:** en **Project Settings → API** copia la **Project URL** y la **anon/public key**.
4. **Configura `local.properties`** en la raíz del proyecto (usa [`local.properties.example`](local.properties.example) como plantilla):
   ```properties
   supabase.url=https://TU-PROYECTO.supabase.co
   supabase.anon_key=TU-CLAVE-ANON
   ```
5. **Gradle Sync y Run ▶️.** Verás tus operadores, canales, mensajes y audios aparecer en el dashboard de Supabase en tiempo real.

> 💡 Dos dispositivos (o emuladores) con el mismo proyecto Supabase se escuchan entre sí al instante por la frecuencia activa.

---

## 🔐 Inicio de sesión (Supabase Auth)

La app abre con un **splash animado** (2 s) y luego pide iniciar sesión con **correo electrónico**: escribes tu correo, te llega un **código de 6 dígitos** y entras — sin contraseña, sin tarjetas y sin consolas externas. Es 100% gratis porque usa el servicio de correo que Supabase ya incluye.

Configuración (ya viene casi todo activado por defecto):
1. **Authentication → Sign In / Providers → Email: ON** ✓
2. Desactiva **"Confirm email"** para entrar directo con el código.
3. Listo. (Plan gratuito: ~3 correos/hora con el SMTP por defecto; para producción se conecta un SMTP propio.)

> 🔒 La sesión queda guardada en el dispositivo: al reabrir, el splash pasa directo a la app. El botón de cerrar sesión (rojo) está en la pantalla principal.

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

## 📡 Backend WebSocket local (opcional / fallback)

Este servidor solo es necesario si **no** configuraste Supabase arriba (la app lo detecta automáticamente). Se encuentra en la carpeta `backend/`:

```bash
cd backend
npm install
npm start
```

El servidor quedará activo en el puerto `3000` (`ws://localhost:3000/ws`). En el emulador de Android se conecta automáticamente a `ws://10.0.2.2:3000/ws`.
