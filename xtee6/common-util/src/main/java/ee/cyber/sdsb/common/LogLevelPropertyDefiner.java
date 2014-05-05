package ee.cyber.sdsb.common;

import ch.qos.logback.core.PropertyDefinerBase;

public class LogLevelPropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        return SystemProperties.getSDSBLogLevel();
    }

}
