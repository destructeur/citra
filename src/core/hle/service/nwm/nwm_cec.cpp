// Copyright 2016 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#include "core/hle/service/nwm/nwm_cec.h"

namespace Service::NWM {

NWM_CEC::NWM_CEC() : ServiceFramework("nwm::CEC") {
    static const FunctionInfo functions[] = {
        {0x000D0082, nullptr, "SendProbeRequest"},
    };
    RegisterHandlers(functions);
}

} // namespace Service::NWM
