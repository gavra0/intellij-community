// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package git4idea.ift

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.actions.GitInit
import git4idea.commands.Git
import git4idea.index.actions.runProcess
import git4idea.repo.GitRepositoryManager
import training.project.FileUtils
import training.project.ProjectUtils
import java.io.File

object GitProjectUtil {
  private const val remoteProjectName = "RemoteLearningProject"

  fun restoreGitLessonsFiles(project: Project) {
    val learningProjectRoot = refreshAndGetProjectRoot(project)
    val gitProjectRoot = invokeAndWaitIfNeeded {
      runWriteAction {
        learningProjectRoot.findChild("git")?.apply {
          findChild(".git")?.delete(this)
        } ?: learningProjectRoot.createChildDirectory(this, "git")
      }
    }

    copyGitProject(gitProjectRoot.toNioPath().toFile()).also {
      if (it) {
        GitInit.refreshAndConfigureVcsMappings(project, gitProjectRoot, gitProjectRoot.path)
      }
      else error("Failed to copy git project")
    }
  }

  fun createRemoteProject(remoteName: String, project: Project): File {
    val root = reCreateRemoteProjectDir()
    if (copyGitProject(root)) {
      configureRemote(remoteName, root, project)
      return root
    }
    error("Failed to create remote project at path: ${root.path}")
  }

  private fun reCreateRemoteProjectDir(): File {
    val projectsRoot = ProjectUtils.learningProjectsPath.toFile()
    val remoteProjectRoot = projectsRoot.listFiles()?.find { it.name == remoteProjectName }.let {
      it?.apply { deleteRecursively() } ?: File(projectsRoot.absolutePath + File.separator + remoteProjectName)
    }
    remoteProjectRoot.mkdir()
    return remoteProjectRoot
  }

  private fun copyGitProject(destination: File): Boolean {
    // Classloader of Git IFT plugin is required here
    val gitProjectURL = this.javaClass.classLoader.getResource("learnProjects/GitProject") ?: error("GitProject not found")
    return FileUtils.copyResourcesRecursively(gitProjectURL, destination)
  }

  private fun configureRemote(remoteName: String, remoteProjectRoot: File, project: Project) {
    runProcess(project, "", false) {
      val git = Git.getInstance()
      val repository = GitRepositoryManager.getInstance(project).repositories.first()
      git.addRemote(repository, remoteName, remoteProjectRoot.path).throwOnError()
      repository.update()
      git.fetch(repository, repository.remotes.first(), emptyList()).throwOnError()
      git.setUpstream(repository, "$remoteName/main", "main").throwOnError()
      repository.update()
    }
  }

  private fun refreshAndGetProjectRoot(project: Project): VirtualFile {
    val learningProjectPath = ProjectUtils.getProjectRoot(project).toNioPath()
    return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(learningProjectPath)
           ?: error("Learning project not found")
  }
}