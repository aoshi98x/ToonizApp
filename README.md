# 🎬 ToonizApp

**ToonizApp** es una potente aplicación nativa de animación 2D en cuadros (frame-by-frame) para Android, desarrollada con **Jetpack Compose** y impulsada por un motor de rendimiento ultra-rápido escrito en **Rust**.

---

## 🚀 Características Principales

* ✏️ **Lienzo de Dibujo Vectorial Interactivo**:
  * Pincel con grosor y opacidad regulables.
  * Herramienta de borrador por radio.
  * Sensibilidad a la presión y fluidez de trazo en tiempo real.
  * Función de Deshacer (Undo) y Rehacer (Redo).

* 🎬 **Timeline y Capas Avanzadas**:
  * Línea de tiempo intuitiva para controlar fotogramas clave.
  * Soporte para múltiples capas de animación por fotograma.
  * Gestión de exposición de fotogramas, duplicado y eliminación.
  * Control de reproducción en tiempo real con ajuste de FPS (cuadros por segundo).

* 🧅 **Piel de Cebolla (Onion Skinning)**:
  * Superposición de fotogramas anteriores y posteriores para referencia de movimiento.
  * Colores y opacidad personalizables para fotogramas pasados y futuros.

* 🎨 **Herramientas de Color**:
  * Rueda de colores interactiva y paletas de colores integradas.
  * Diálogo de selección de color preciso.

* 📤 **Exportación de Proyectos**:
  * Exportación directa a video **MP4**.
  * Exportación a secuencia de imágenes comprimida en formato **ZIP**.

* ⚡ **Core Nativo en Rust (`rust_core`)**:
  * Lógica de cálculo de vectores, almacenamiento de trazos y procesamiento de capas desarrollado en Rust para máxima velocidad y eficiencia de memoria.
  * Interoperabilidad fluida mediante **UniFFI** y **JNA**.

* 📁 **Gestor de Proyectos**:
  * Creación y administración de múltiples proyectos.
  * Configuración personalizada de resolución (Ancho x Alto) y FPS.
  * Vista previa con miniaturas.

---

## 🛠️ Arquitectura y Tecnologías

* **Lenguaje Principal**: [Kotlin](https://kotlinlang.org/)
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Motor Nativo**: [Rust](https://www.rust-lang.org/)
* **FFI / Interoperabilidad**: [UniFFI](https://github.com/mozilla/uniffi-rs) & [JNA (Java Native Access)](https://github.com/java-native-access/jna)
* **Arquitectura UI**: MVVM (Model-View-ViewModel) con StateFlow y Coroutines
* **Procesamiento Multimedia**: Android MediaCodec / MediaMuxer (`Mp4ExportManager`)
* **Compatibilidad Android**:
  * `minSdk`: 26 (Android 8.0 Oreo)
  * `targetSdk`: 37

---

## 📂 Estructura del Proyecto

```text
com.colombianapps.toonizapp/
├── data/
│   ├── model/           # Modelos de datos (AnimationProject)
│   └── repository/      # Repositorio de persistencia de proyectos
├── ui/
│   ├── components/      # Componentes UI reutilizables (AnimationCanvas, TimelineView, TopToolbar, etc.)
│   ├── screen/          # Pantallas principales (SplashScreen, ProjectManagerScreen, AnimationStudioScreen, ExportScreen)
│   ├── theme/           # Colores, tipografía y tema de Compose (Material 3)
│   └── viewmodel/       # Lógica de estado (StudioViewModel, DrawingManager)
├── util/                # Módulos de exportación (Mp4ExportManager, ZipExportManager)
└── uniffi/rust_core/    # Bindings generados en Kotlin para la librería Rust nativa
```

---

## ⚙️ Requisitos de Compilación

1. **Android Studio**: Android Studio Ladybug | 2024.2.1 o superior.
2. **JDK**: Java 17 o superior.
3. **Rust Toolchain** *(Requerido para compilar `rustCore`)*:
   * Instala Rust desde [rustup.rs](https://rustup.rs/).
   * Instala `cargo-ndk`:
     ```bash
     cargo install cargo-ndk
     ```
   * Agrega los targets de Android para Rust:
     ```bash
     rustup target add aarch64-linux-android x86_64-linux-android
     ```

---

## 🚀 Compilación e Instalación

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/tu-usuario/ToonizApp.git
   cd ToonizApp
   ```

2. **Compilar los bindings nativos de Rust**:
   Si realizas cambios en el módulo `rustCore`, ejecuta la tarea Gradle correspondiente:
   ```bash
   ./gradlew generateUniFFIBindings
   ```

3. **Ejecutar en Android Studio**:
   * Abre el proyecto en Android Studio.
   * Sincroniza los archivos de Gradle (*Gradle Sync*).
   * Conecta un dispositivo físico o emulador con Android 8.0+ y presiona **Run** (`Shift + F10`).

---

## 📝 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para obtener más información.

---

Desarrollado por **ColombianApps**.
