## App Architecture

<img src="https://github.com/ologe/canaree-music-player/blob/master/docs/images/app_architecture.jpg">

<br>

### Legend
- Every rectangle is an Android `:module`
- Every vertical line delimits a `logic layer`
- To avoid arrows hell, only some important dependencies is highlighted
- A `:module` in a `layer`:
    - can't depend on other modules in the same layer (except `shared layer`)
    - can depend on any module in any layer on it's upper right, or on the layer below

## Modules

#### `:app`
- Must depend on almost every `:module` in order to build the apk
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/app/build.gradle)

#### `:core`
- Contains gateway that allow decoupled communication between `:data` and other modules
- Contains common entities
- Contains interactors (business use-cases)
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/core/build.gradle)

#### `:presentation`
- Self explanatory
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/presentation/build.gradle)

#### `:service-music`
- Self explanatory
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/service-music/build.gradle)

#### `:service-floating`
- Self explanatory
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/service-floating/build.gradle)

#### `:image-provider`
- Handles all image loading (from local storage and network) and caching
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/image-provider/build.gradle)

#### `:injection`
- Creates dagger shared core component
- Binds `:core` gateways with `:data` implementations
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/injection/build.gradle) 

#### `:data`
- Repositories implementation
- Makes network calls and caching
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/data/build.gradle)

## Libs

#### `:media`
- Provides a reactive API to connect to `:service-music`
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/media/build.gradle)

#### `:equalizer`
- Equalizer, BassBoos and Virtualizer implementation for different API level
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/equalizer/build.gradle)

#### `:feature_stylize`
- On-demand dynamic module
- Used from image blending/stylize
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/feature_stylize/build.gradle)

#### `:offline-lyrics`
- Provides an API to read offline lyrics, saved on device on from track metadata
- Supports `.lrc` file format for synced lyrics
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/offline-lyrics/build.gradle) 

#### `:jaudiotagger`
- Allows to read and update tracks metadata
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/jaudiotagger/build.gradle) 

## Utils

#### `:intents`
- Contains common actions and constants for communication between modules
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/intents/build.gradle)

#### `:prefs-keys`
- Contains shared preferences keys, using a separate module for communication between modules
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/prefs-keys/build.gradle)

#### `:shared`
- Shared pure java/kotlin utilities
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/shared/build.gradle)

#### `:shared-android`
- Self explanatory
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/shared-android/build.gradle) 

#### `:shared-widgets`
- Self explanatory
- [build.gradle](https://github.com/ologe/canaree-music-player/blob/master/shared-widgets/build.gradle)