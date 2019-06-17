package me.thetd.TCUtils;

import me.thetd.tcutils.*;

class Initializer implements ModuleInitializer {

    @Override
    public void initializeModules(TCUtilsModuleManager moduleManager) {
        moduleManager.add(AutoRespawn.class, false, "ProtocolLib");
        moduleManager.add(MsgFix.class, false);
        moduleManager.add(AutoSave.class, false);
        moduleManager.add(PublicMinecart.class, false);
    }
}
