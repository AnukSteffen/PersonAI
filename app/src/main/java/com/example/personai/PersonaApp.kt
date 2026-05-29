package com.example.personai

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PersonaApp : Application(), ImageLoaderFactory {

    // 全局配置 Coil ImageLoader
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // 1. 支持 GIF
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // 2. 支持视频缩略图
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}