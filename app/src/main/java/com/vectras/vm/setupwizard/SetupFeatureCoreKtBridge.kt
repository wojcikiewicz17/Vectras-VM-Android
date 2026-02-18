package com.vectras.vm.setupwizard

object SetupFeatureCoreKtBridge {
    @JvmStatic
    fun isPkgInstalled(pkgDb: String?, pkgName: String?): Boolean {
        if (pkgDb.isNullOrEmpty() || pkgName.isNullOrEmpty()) {
            return false
        }

        val lines = pkgDb.split(Regex("\\R"))
        for (line in lines) {
            if (!line.startsWith("P:")) {
                continue
            }

            val installedPkg = line.substring(2)
            if (installedPkg == pkgName) {
                return true
            }
        }

        return false
    }
}
