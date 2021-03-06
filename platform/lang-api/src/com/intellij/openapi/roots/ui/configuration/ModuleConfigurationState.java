/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.util.UserDataHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface ModuleConfigurationState extends UserDataHolder {
  ModulesProvider getModulesProvider();
  FacetsProvider getFacetsProvider();

  /**
   * Returns an instance which can be used to modify the module configuration
   */
  ModifiableRootModel getModifiableRootModel();

  /**
   * Returns the actual state of the module configuration
   */
  ModuleRootModel getCurrentRootModel();

  @NotNull
  Project getProject();

  /**
   * @deprecated use {@link #getModifiableRootModel()}} if you need to modify the model and use {@link #getCurrentRootModel()} if you just
   * need to read the current state
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2022.2")
  @Deprecated
  default ModifiableRootModel getRootModel() {
    return getModifiableRootModel();
  }
}