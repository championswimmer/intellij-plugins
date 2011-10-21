package com.intellij.lang.javascript.flex.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.FlexModuleType;
import com.intellij.lang.javascript.flex.projectStructure.model.*;
import com.intellij.lang.javascript.flex.projectStructure.options.BuildConfigurationNature;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.javascript.flex.run.AirMobileRunnerParameters.AirMobileRunTarget;

public class FlexIdeRunConfiguration extends RunConfigurationBase implements RunProfileWithCompileBeforeLaunchOption {

  private FlexIdeRunnerParameters myRunnerParameters = new FlexIdeRunnerParameters();

  public FlexIdeRunConfiguration(final Project project, final ConfigurationFactory factory, final String name) {
    super(project, factory, name);
  }

  public FlexIdeRunConfiguration clone() {
    final FlexIdeRunConfiguration clone = (FlexIdeRunConfiguration)super.clone();
    clone.myRunnerParameters = myRunnerParameters.clone();
    return clone;
  }

  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new FlexIdeRunConfigurationForm(getProject());
  }

  public JDOMExternalizable createRunnerSettings(final ConfigurationInfoProvider provider) {
    return null;
  }

  public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor(final ProgramRunner runner) {
    return null;
  }

  @Override
  public void readExternal(final Element element) throws InvalidDataException {
    super.readExternal(element);
    myRunnerParameters = new FlexIdeRunnerParameters();
    XmlSerializer.deserializeInto(myRunnerParameters, element);
  }

  @Override
  public void writeExternal(final Element element) throws WriteExternalException {
    super.writeExternal(element);
    XmlSerializer.serializeInto(myRunnerParameters, element);
  }

  public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
    final FlexIdeBuildConfiguration config;
    try {
      config = BCBasedRunnerParameters.checkAndGetModuleAndBC(getProject(), myRunnerParameters).second;
    }
    catch (RuntimeConfigurationError e) {
      throw new ExecutionException(e.getMessage());
    }

    final BuildConfigurationNature nature = config.getNature();
    if (nature.isDesktopPlatform() ||
        (nature.isMobilePlatform() && myRunnerParameters.getMobileRunTarget() == AirMobileRunTarget.Emulator)) {
      final AirRunState airRunState = new AirRunState(env);
      airRunState.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
      return airRunState;
    }

    return FlexBaseRunner.EMPTY_RUN_STATE;
  }

  public void checkConfiguration() throws RuntimeConfigurationException {
    checkAndGetModuleAndBC(getProject(), myRunnerParameters);
  }

  public static Pair<Module, FlexIdeBuildConfiguration> checkAndGetModuleAndBC(final Project project,
                                                                               final FlexIdeRunnerParameters params) throws RuntimeConfigurationError {
    final Pair<Module,FlexIdeBuildConfiguration> moduleAndBC = BCBasedRunnerParameters.checkAndGetModuleAndBC(project, params);

    final FlexIdeBuildConfiguration bc = moduleAndBC.second;

    if (bc.getOutputType() != OutputType.Application) {
      throw new RuntimeConfigurationError(
        FlexBundle.message("bc.does.not.produce.app", params.getBCName(), params.getModuleName()));
    }

    if (bc.getTargetPlatform() == TargetPlatform.Mobile) {
      if (params.getMobileRunTarget() == AirMobileRunTarget.AndroidDevice && !bc.getAndroidPackagingOptions().isEnabled()) {
        throw new RuntimeConfigurationError(
          FlexBundle.message("android.disabled.in.bc", params.getBCName(), params.getModuleName()));
      }
    }

    return moduleAndBC;
  }

  @NotNull
  public FlexIdeRunnerParameters getRunnerParameters() {
    return myRunnerParameters;
  }

  @NotNull
  public Module[] getModules() {
    final Module module = ModuleManager.getInstance(getProject()).findModuleByName(myRunnerParameters.getModuleName());
    if (module != null && ModuleType.get(module) instanceof FlexModuleType) {
      return new Module[]{module};
    }
    else {
      return Module.EMPTY_ARRAY;
    }
  }

  private class AirRunState extends CommandLineState {

    public AirRunState(ExecutionEnvironment env) {
      super(env);
    }

    @NotNull
    protected OSProcessHandler startProcess() throws ExecutionException {
      final FlexIdeBuildConfiguration config;
      try {
        config = BCBasedRunnerParameters.checkAndGetModuleAndBC(getProject(), myRunnerParameters).second;
      }
      catch (RuntimeConfigurationError e) {
        throw new ExecutionException(e.getMessage());
      }
      return JavaCommandLineStateUtil.startProcess(FlexBaseRunner.createAdlCommandLine(myRunnerParameters, config));
    }
  }
}
