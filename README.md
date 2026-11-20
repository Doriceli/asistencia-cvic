# Asistencia CVIC (Android)

App para la **Galaxy Tab S8 (SM-X700, One UI 8, Android 16)** que:

1. Lleva la **Tabla de Asistencia** con el S Pen: escribes en CVIC, CONSEJEROS e Invitados, y **TOTAL** (la suma de las tres) y **Hora** (la hora del último cambio en esa columna) se llenan solos. La fila de **CONSEJEROS** siempre está resaltada. Empieza con 1 tabla y puedes agregar más.
2. Permite editar la lista de **integrantes de la CVIC** (agregar, quitar, renombrar y reordenar).
3. Genera la **plantilla para Samsung Notes** (PDF o PNG) con la lista actual, y la guarda en **Descargas › Plantillas CVIC**.

Funciona **sin internet**: la página, las fuentes y el generador de PDF van dentro de la app.

---

## 1. Qué instalar en la computadora

| Qué | Dónde |
|---|---|
| **Android Studio** (versión estable más reciente) | https://developer.android.com/studio |
| Cable USB-C para la tableta | — |
| Cuenta de GitHub (solo si el repositorio es privado) | https://github.com |

Android Studio ya trae el JDK y el SDK. La primera vez descargará Gradle 9.8.2 y el **SDK de Android 37**: acepta cuando lo pida.

## 2. Preparar la tableta (solo la primera vez)

1. **Ajustes → Acerca de la tableta → Información de software** y toca **7 veces "Número de compilación"**. Aparecerá "Modo desarrollador activado".
2. **Ajustes → Opciones de desarrollador** y activa **Depuración USB**.
3. Conecta la tableta a la computadora y, en la tableta, acepta **"¿Permitir depuración USB?"** (marca "Permitir siempre desde esta computadora").

## 3. Descargar el proyecto desde GitHub e instalar la app

1. Abre Android Studio. En la pantalla de bienvenida toca **Clone Repository** (con un proyecto abierto: **File → New → Project from Version Control**).
2. En **URL** pega:

   ```
   https://github.com/Doriceli/asistencia-cvic.git
   ```

   Elige la carpeta donde se guardará y pulsa **Clone**. Si el repositorio es privado, inicia sesión con tu cuenta de GitHub cuando lo pida (**Log In via GitHub**).
3. Pulsa **Trust Project** y espera a que termine **Gradle Sync** (barra inferior). La primera vez tarda varios minutos.
4. Arriba, en la lista de dispositivos, elige **Samsung SM-X700**.
5. Pulsa **▶ Run 'app'**. La app **Asistencia CVIC** se instala y se abre en la tableta.

Sin Android Studio puedes bajar el proyecto como ZIP desde GitHub (**Code → Download ZIP**), descomprimirlo y abrirlo con **File → Open**.

Para recibir cambios nuevos del repositorio: **Git → Pull** y otra vez **▶ Run**. Tus datos se conservan.

### Instalar sin cable (opcional)
**Build → Generate App Bundles or APKs → Generate APKs**. Cuando termine, toca **locate** y copia `app-debug.apk` a la tableta (por Quick Share o cable). Ábrelo en la tableta y permite "Instalar apps desconocidas" para Mis Archivos.

## 4. Usar la plantilla en Samsung Notes

1. En la app, pestaña **Plantilla para Notes**: elige cuántas tablas lleva (1 a 4) y toca **Guardar PDF**.
2. En Samsung Notes: nota nueva → **Estilo de nota → Personalizado → Seleccionar archivo**.
3. Elige el PDF en **Descargas › Plantillas CVIC**.

Vuelve a guardar la plantilla cada vez que cambie la lista de integrantes o empiece una sesión nueva.

## 5. Dónde se guardan los datos

Las sesiones y la lista de integrantes se guardan **dentro de la app** en la tableta, cada vez que escribes algo.

- **Desinstalar la app borra los datos.** Antes de desinstalarla o de cambiar de tableta, ve a **Integrantes CVIC → Guardar respaldo**. El archivo `.json` queda en Descargas › Plantillas CVIC.
- Para recuperar los datos: **Integrantes CVIC → Restaurar respaldo** y elige ese archivo.
- Instalar una versión nueva con ▶ Run **no** borra los datos.

## 6. Qué se puede cambiar en el código

| Qué | Dónde |
|---|---|
| Lista original de integrantes (la de "Restaurar lista original") | `app/src/main/assets/index.html`, constante `DEFAULT_MEMBERS` |
| Colores de la plantilla (verde salvia, gris de CONSEJEROS, flores) | `index.html`, constante `PAL` |
| Medidas de la plantilla (ancho de 2560 px, alto de filas) | `index.html`, función `drawTemplate` |
| Carpeta de Descargas donde se guardan los archivos | `MainActivity.kt`, `CARPETA` |
| Nombre de la app | `app/src/main/res/values/strings.xml` |

Si cambias la lista en `DEFAULT_MEMBERS`, la lista que ya está en la tableta no cambia. Para aplicarla, ve a **Integrantes CVIC → Restaurar lista original**, o edítala directo en la app.

## 7. Estructura

```
AsistenciaCVIC/
├── app/
│   ├── build.gradle.kts            ← sin dependencias externas
│   └── src/main/
│       ├── AndroidManifest.xml     ← sin permisos
│       ├── assets/
│       │   ├── index.html          ← toda la app (tablas, integrantes, plantilla)
│       │   └── fonts/              ← Mali e Inter (licencia SIL OFL 1.1)
│       ├── java/mx/cvic/asistencia/MainActivity.kt ← WebView + guardar PDF/PNG/respaldo
│       └── res/                    ← nombre, tema e icono
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── gradlew, gradle/wrapper/        ← Gradle 9.8.2
```

### Versiones

| Componente | Versión |
|---|---|
| Android Gradle Plugin | 9.5.0 (Kotlin integrado) |
| Gradle | 9.8.2 |
| compileSdk / targetSdk / minSdk | 37 / 35 / 29 |

Si Android Studio ofrece actualizar el AGP con **AGP Upgrade Assistant**, puedes aceptar.

## Solución de problemas

- **La tableta no aparece en Android Studio:** desconecta y vuelve a conectar el cable, y acepta el aviso de depuración USB. Usa un cable de datos, no solo de carga.
- **"SDK location not found":** abre **File → Project Structure → SDK Location** y elige el SDK que instaló Android Studio. Otra opción es **Tools → SDK Manager** e instalar "Android API 37".
- **Gradle pide otro JDK:** **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** y elige "jbr-21" (el que viene con Android Studio).
- **El S Pen no convierte a texto:** en la tableta ve a **Ajustes → Funciones avanzadas → S Pen** y activa **"Escritura a mano del S Pen a texto"**.
