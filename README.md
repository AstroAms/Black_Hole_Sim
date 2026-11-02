# Relativistic Black Hole Simulation (General Relativity)

Inspired by Kavan's **"Simulating Black Holes in C++"** ([YouTube Video](https://youtu.be/8-B6ryuBkCM)).

---

## 🚀 How to Run

### Option 1: 3D Relativistic Simulation (Full Real-Time Ray-Tracer)
Double-click `run_3D_simulation.bat` or run in terminal:
```bash
cd blackhole-sim
mvn exec:java
```

### Option 2: 2D Gravitational Lensing Simulation (Photon Trails & Horizon Capture)
Double-click `run_2D_simulation.bat` or run in terminal:
```bash
java BlackHoleSimulation
```

---

## 🎮 3D Simulation Controls

| Key / Action | Effect |
| :--- | :--- |
| **Left Mouse Drag** | Orbit camera 360° smoothly around the black hole |
| **Mouse Scroll** | Zoom in / out (from wide overview to photon sphere close-up) |
| **[D]** | Toggle **Accretion Disk** on/off |
| **[L]** | Toggle **Relativistic Gravitational Lensing** (compare curved vs. flat Euclidean light) |
| **[B]** | Toggle **Relativistic Doppler Beaming** (approaching side blue-boosted, receding side dimmed) |
| **[G]** | Toggle embedded **Spacetime Curvature Grid** shader |
| **[M]** | Toggle 3D **Spacetime Curvature Funnel Mesh** (Flamm's paraboloid) |
| **[S]** | Toggle **Background Starfield & Nebulae** |
| **[SPACE]** | Pause / Resume rotation and accretion flow |
| **[1]** | Preset View 1: **Equatorial** (edge-on, showing iconic upper and lower lensed arcs) |
| **[2]** | Preset View 2: **30° Cinematic Inclination** (*Interstellar* Gargantua perspective) |
| **[3]** | Preset View 3: **Polar Top-Down** (vortex view directly above the black hole) |
| **[4]** | Preset View 4: **Close-up on Photon Ring** ($r = 1.5 r_s$) |
| **[R]** | Reset camera to default position |
| **[UP] / [DOWN]** | Increase / Decrease Black Hole mass & event horizon radius |
| **[LEFT] / [RIGHT]** | Adjust Accretion Disk glow intensity |
| **[ESC]** | Exit simulation |

---

## 🔬 Physics Implemented

1. **Schwarzschild Metric & Null Geodesics**:
   - Light paths follow null geodesics around a non-rotating mass $M$ with Schwarzschild radius $r_s = \frac{2GM}{c^2}$.
   - Exact relativistic acceleration: $\vec{a} = -\frac{3}{2} r_s \frac{|\vec{L}|^2}{r^5} \vec{r}$.
   - Photon sphere at $r_{ph} = 1.5 r_s$.
   - Critical shadow impact parameter $b_{crit} = \frac{\sqrt{27}}{2} r_s \approx 2.598 r_s$.

2. **Accretion Disk & Gravitational Lensing**:
   - Thin equatorial accretion disk from ISCO ($2.4 r_s$) to $7.5 r_s$.
   - Rays looping over and under the horizon are bent toward the observer, producing the double-halo arch.
   - Shakura-Sunyaev temperature profile: $T(r) \propto r^{-3/4} (1 - \sqrt{r_{in}/r})^{1/4}$.
   - Dynamic differential Keplerian rotation ($\Omega \propto r^{-1.5}$) with turbulent plasma filaments.

3. **Relativistic Doppler Beaming & Gravitational Redshift**:
   - Plasma moving toward the observer at relativistic speed ($\beta = v/c = \sqrt{\frac{r_s}{2r}}$) is Doppler-boosted by $\delta^3$, appearing significantly brighter and bluer.
   - Receding plasma is dimmed and shifted toward deep crimson red.
   - Light climbing out of the gravitational potential undergoes gravitational redshift $g = \sqrt{1 - \frac{r_s}{r}}$.

4. **Spacetime Curvature (Flamm's Paraboloid)**:
   - 3D spatial embedding of Schwarzschild geometry: $y(r) = -2.0 \sqrt{r_s (r - r_s)}$.
   - Interactive wireframe funnel visualizing the depression in the fabric of spacetime.

---

## 📁 Project Structure

```
New folder (2)/
├── run_3D_simulation.bat       # One-click launcher for 3D LWJGL simulation
├── run_2D_simulation.bat       # One-click launcher for 2D lensing simulation
├── README.md                   # Documentation and user guide
├── BlackHoleSimulation.java    # 2D Relativistic Light Deflection Simulation
├── BlackHoleSimulation.class   # Compiled 2D class
├── blackhole-sim/              # 3D LWJGL OpenGL Simulation (Maven project)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/blackhole/
│       │   ├── Main.java           # Window, camera orbit, event loop
│       │   ├── GridUtils.java      # Screen quad & Flamm paraboloid funnel
│       │   ├── ShaderUtils.java    # Shader loading and compilation
│       │   ├── TextureUtils.java   # STBImage texture loading
│       │   └── ShaderProgram.java  # Shader wrapper
│       └── resources/
│           ├── shaders/
│           │   ├── vertex.glsl         # Fullscreen quad vertex shader
│           │   ├── fragment.glsl       # Relativistic ray-marching shader
│           │   ├── grid_vertex.glsl    # 3D Flamm funnel vertex shader
│           │   └── grid_fragment.glsl  # 3D Flamm funnel fragment shader
│           └── textures/
│               └── stars.png           # Background starfield texture
└── cpp-source/                 # Original C++ source code & CMake files (Kavan)
```

## 📋 Requirements

| Component | Minimum Version |
| :--- | :--- |
| Java JDK | 17 or newer |
| Apache Maven | 3.8+ (3D simulation only) |
| GPU | OpenGL 3.3+ compatible |
| OS | Windows 10/11, Linux, macOS |

## 🛠️ Troubleshooting

| Problem | Solution |
| :--- | :--- |
| `mvn` is not recognized | Install Maven and add it to your `PATH` |
| `java` is not recognized | Install JDK 17+ and set `JAVA_HOME` |
| Black screen or crash on launch | Update your GPU drivers; ensure OpenGL 3.3+ support |
| Low FPS in 3D mode | Toggle off the starfield (**S**) or spacetime grid (**G**) |
| macOS window fails to open | Run with `-XstartOnFirstThread` JVM flag |


## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

Ideas for future improvements:
- Kerr (rotating) black hole metric
- Gravitational wave ripple visualization
- Screenshot / video recording hotkey
- Adjustable observer distance via UI sliders
- Gravitational time dilation visualizer

## 🙏 Credits

- **Kavan** for the original [Simulating Black Holes in C++](https://youtu.be/8-B6ryuBkCM) tutorial and source code
- [LWJGL](https://www.lwjgl.org/) for the Java OpenGL bindings
- Schwarzschild, Flamm, and Shakura-Sunyaev for the underlying physics