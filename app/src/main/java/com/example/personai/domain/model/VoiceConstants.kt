package com.example.personai.domain.model

object VoiceConstants {
    val VOICE_PRESETS = mapOf(
        "zh_female_wanqudashu_moon_bigtts" to "湾区大叔",
        "zh_female_daimengchuanmei_moon_bigtts" to "呆萌川妹",
        "zh_male_guozhoudege_moon_bigtts" to "广州德哥",
        "zh_male_beijingxiaoye_moon_bigtts" to "北京小爷",
        "zh_male_shaonianzixin_moon_bigtts" to "少年梓辛/Brayan",
        "zh_female_meilinvyou_moon_bigtts" to "魅力女友",
        "zh_male_shenyeboke_moon_bigtts" to "深夜播客",
        "zh_female_sajiaonvyou_moon_bigtts" to "柔美女友",
        "zh_female_yuanqinvyou_moon_bigtts" to "撒娇学妹",
        "zh_male_haoyuxiaoge_moon_bigtts" to "浩宇小哥",
        "zh_male_guangxiyuanzhou_moon_bigtts" to "广西远舟",
        "zh_female_meituojieer_moon_bigtts" to "妹坨洁儿",
        "zh_male_yuzhouzixuan_moon_bigtts" to "豫州子轩",
        "zh_female_linjianvhai_moon_bigtts" to "邻家女孩", //通用女声
        "zh_female_gaolengyujie_moon_bigtts" to "高冷御姐",
        "zh_male_yuanboxiaoshu_moon_bigtts" to "渊博小叔",
        "zh_male_yangguangqingnian_moon_bigtts" to "阳光青年", //通用男声
        "zh_male_aojiaobazong_moon_bigtts" to "傲娇霸总",
        "zh_male_jingqiangkanye_moon_bigtts" to "京腔侃爷/Harmony",
        "zh_female_shuangkuaisisi_moon_bigtts" to "爽快思思/Skye"
    )

    // 获取默认音色 ID
    const val DEFAULT_VOICE_ID = "zh_female_linjianvhai_moon_bigtts"

    // 根据名字反查 ID
    fun getIdByName(name: String): String {
        return VOICE_PRESETS.entries.find { it.value.contains(name) }?.key ?: DEFAULT_VOICE_ID
    }
}