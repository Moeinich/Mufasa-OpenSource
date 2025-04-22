package scripts;

import UI.ScriptConfigurationUI;
import UI.scripts.AnnotationControls;
import helpers.AbstractScript;
import helpers.annotations.ScriptConfiguration;
import helpers.annotations.ScriptTabConfiguration;
import helpers.scripts.utils.ScriptConfigurationWrapper;
import utils.CredentialsManager;

import java.util.Arrays;
import java.util.Map;

public class ScriptConfigurator {
    private final ScriptAccountManager scriptAccountManager;
    private final CredentialsManager credMgr;
    private final AnnotationControls annotationControls;

    public ScriptConfigurator(AnnotationControls annotationControls, ScriptAccountManager scriptAccountManager, CredentialsManager credMgr) {
        this.annotationControls = annotationControls;
        this.scriptAccountManager = scriptAccountManager;
        this.credMgr = credMgr;
    }

    public boolean setupScriptConfiguration(AbstractScript script, String deviceID) {
        // Get script configurations
        ScriptConfigurationWrapper wrapper = getScriptConfigurations(script.getClass());

        // If there are configurations, let the user decide on those
        ScriptConfigurationUI configUI = new ScriptConfigurationUI(
                annotationControls,
                credMgr,
                wrapper,
                script.toString()
        );

        Map<String, String> chosenConfigurations = configUI.showAndGetConfigurations();

        // If user closed the setup UI without choosing any options
        if (chosenConfigurations == null || chosenConfigurations.isEmpty()) {
            return false;
        }

        // Fetch the selected account
        String selectedAccount = chosenConfigurations.get("selectedAccount");
        System.out.println("Chosen accounts: " + selectedAccount);
        System.out.println("Chosen device: " + deviceID);
        if (selectedAccount != null) {
            scriptAccountManager.setAccountForEmulator(deviceID, selectedAccount);
        }

        script.setConfigurations(chosenConfigurations); // Parse the configurations back to the script
        return true;
    }

    private ScriptConfigurationWrapper getScriptConfigurations(Class<? extends AbstractScript> scriptClass) {
        ScriptConfigurationWrapper wrapper = new ScriptConfigurationWrapper();

        // Handle standalone configurations
        if (scriptClass.isAnnotationPresent(ScriptConfiguration.List.class)) {
            wrapper.getStandaloneConfigurations().addAll(
                    Arrays.asList(scriptClass.getAnnotation(ScriptConfiguration.List.class).value())
            );
        }

        // Handle grouped configurations inside ScriptTabConfiguration.List
        if (scriptClass.isAnnotationPresent(ScriptTabConfiguration.List.class)) {
            ScriptTabConfiguration.List tabConfigList = scriptClass.getAnnotation(ScriptTabConfiguration.List.class);
            for (ScriptTabConfiguration tabConfig : tabConfigList.value()) {
                wrapper.getTabGroups().add(new ScriptConfigurationWrapper.ScriptTabGroup(
                        tabConfig.name(),
                        Arrays.asList(tabConfig.configurations())
                ));
            }
        } else if (scriptClass.isAnnotationPresent(ScriptTabConfiguration.class)) {
            // Handle single ScriptTabConfiguration annotation
            ScriptTabConfiguration tabConfig = scriptClass.getAnnotation(ScriptTabConfiguration.class);
            wrapper.getTabGroups().add(new ScriptConfigurationWrapper.ScriptTabGroup(
                    tabConfig.name(),
                    Arrays.asList(tabConfig.configurations())
            ));
        }

        return wrapper;
    }
}
