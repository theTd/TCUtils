package me.thetd.TCUtils;

import me.thetd.tcutils.AutoRespawn;
import me.thetd.tcutils.ModuleInitializer;
import me.thetd.tcutils.MsgFix;
import me.thetd.tcutils.TCUtilsModuleManager;

class Initializer implements ModuleInitializer {

    @Override
    public void initializeModules(TCUtilsModuleManager moduleManager) {
        moduleManager.add(AutoRespawn.class, false, "ProtocolLib");
        moduleManager.add(MsgFix.class, false);
    }
}
