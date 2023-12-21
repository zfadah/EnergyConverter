package group.zfadah.converter

import net.minecraftforge.common.config.Configuration
import java.io.File

open class Config {
    companion object{
        var greeting = "Hello World"
        fun synchronizeConfiguration(configFile: File?) {
            val configuration = Configuration(configFile)
            greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?")
            if (configuration.hasChanged()) {
                configuration.save()
            }
        }
    }



}
