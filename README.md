# ⛏ PROTECTIUM — Plugin de Protecciones Cúbicas

## Regla Central (inmutable por diseño)
1. Un admin ejecuta `/prot dar <tipo> <radio> <jugador>`
2. El jugador recibe un ítem con marca NBT única y sellada.
3. **El ítem NO hace nada mientras esté en el inventario.**
4. Solo al **colocarlo** en el mundo se activa la protección como un cubo.
5. **Romper el bloque = protección eliminada.** Sin excepciones, sin persistencia.

---

## Tipos de Protección

| Tipo | Comando | Qué bloquea |
|---|---|---|
| `area` | `/prot dar area 8 Steve` | Romper, colocar, incendio, pistones |
| `spawn` | `/prot dar spawn 16 Steve` | Spawn de mobs hostiles |
| `entrada` | `/prot dar entrada 12 Steve` | Entrada de jugadores sin bypass |
| `fuego` | `/prot dar fuego 12 Steve` | Propagación de fuego y lava |
| `redstone` | `/prot dar redstone 10 Steve` | Activadores de redstone |

---

## Comandos

```
/prot dar <tipo> <radio> <jugador>   — Da un ítem de protección
/prot lista                          — GUI: lista de protecciones activas
/prot tipos                          — GUI: tipos disponibles
/prot recargar                       — Recarga la configuración
```

---

## Permisos

| Permiso | Descripción | Default |
|---|---|---|
| `protectium.admin` | Acceso administrativo completo | op |
| `protectium.bypass` | Inmune a todas las protecciones | op |

---

## Arquitectura

```
core/           — Bootstrap, mensajes centralizados
protection/     — ProtectionType (enum), ProtectionRecord (valor inmutable), CubeRegion (AABB)
registry/       — ProtectionRegistry (índice thread-safe de protecciones vivas)
item/           — ItemAuthority (creación y validación de ítems NBT)
command/        — ComandoProtectium (dispatch) + SubComando (Strategy pattern)
listener/       — 9 listeners aislados, uno por responsabilidad
fx/             — FxEngine (motor visual) + FxTheme (temas de partículas)
gui/           — GuiManager + GuiHolder + GuiTipo (inventarios custom)
task/           — FxTickTask (renderizado) + ConsistencyTask (limpieza de huérfanas)
```

---

## Compilar

```bash
mvn clean package
```

Requiere Java 17+ y Bukkit 1.21.

---

## Efectos Visuales

- **Al colocar:** Explosión de partículas en las 8 esquinas del cubo + onda de expansión desde el centro + sonido de activación.
- **Activos:** Aristas del cubo pulsan con brillo rítmico. Caras emiten partículas ambientes flotantes.
- **Al romper:** Dispersión masiva desde esquinas + lluvia de partículas desde cada cara + sonido de desactivación.
- **Rebote (entrada):** Impacto visual en el punto de contacto + partículas de choque + sonido.

Cada tipo tiene su propio tema de colores y partículas, configurable en `config.yml`.
