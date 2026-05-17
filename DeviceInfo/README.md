# DeviceInfo — CPU-Z Clone for Android

A native Android app that shows device hardware information in a multi-tab layout,
built with a clean multi-module architecture.

## Modules

| Module           | Purpose                                      |
|------------------|----------------------------------------------|
| `:app`           | Main application, hosts ViewPager2 + TabLayout |
| `:core`          | Shared data models and device utility helpers |
| `:feature-soc`   | SOC/CPU details tab                          |
| `:feature-device`| Device hardware info tab                     |
| `:feature-system`| Android OS & system info tab                 |
| `:feature-battery`| Battery status tab                          |
| `:feature-thermal`| Thermal zones tab                           |
| `:feature-sensors`| Hardware sensors list tab                   |
| `:feature-about` | App info & about tab                         |

## Tech Stack

- **Language:** Kotlin
- **UI:** RecyclerView, ViewPager2, TabLayout, ViewBinding
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)
- **Build System:** Gradle with Version Catalog (libs.versions.toml)

## Setup

1. Open in Android Studio (Hedgehog or newer)
2. Let Gradle sync complete
3. Run on a physical device (recommended for real sensor/thermal data)

## Features

- **SOC Tab:** CPU model, cores, clock speeds per core, GPU info, governor
- **Device Tab:** Model, manufacturer, screen size/resolution/density, RAM, storage
- **System Tab:** Android version, API level, security patch, kernel info, uptime
- **Battery Tab:** Health, level, power source, status, temperature, voltage
- **Thermal Tab:** All thermal zone temperatures
- **Sensors Tab:** All available hardware sensors with live data
- **About Tab:** App version, links, settings

## Note

Real hardware data (CPU frequencies, thermal zones) requires a physical Android device.
Some values may show "Unknown" on emulators.
