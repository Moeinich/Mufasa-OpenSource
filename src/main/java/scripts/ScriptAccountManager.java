package scripts;

import java.util.concurrent.ConcurrentHashMap;

public class ScriptAccountManager {
    private final ConcurrentHashMap<String, String> getSelectedAccount = new ConcurrentHashMap<>();

    public void setAccountForEmulator(String deviceID, String accountName) {
        getSelectedAccount.put(deviceID, accountName);
    }

    public String getSelectedAccount(String deviceID) {
        return getSelectedAccount.get(deviceID);
    }

    public void removeAccountForEmulator(String deviceID) {
        getSelectedAccount.remove(deviceID);
    }

}
